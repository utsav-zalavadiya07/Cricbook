import React, { createContext, useContext, useState, useEffect } from "react";

export interface Action<P = any> {
  type: string;
  payload?: P;
}

export type Reducer<S, A = Action> = (state: S | undefined, action: A) => S;

export interface Slice<S, R extends Record<string, (state: S, action: Action<any>) => void | S>> {
  name: string;
  reducer: Reducer<S, Action<any>>;
  actions: {
    [K in keyof R]: (payload?: any) => Action<any>;
  };
  initialState: S;
}

export function createSlice<
  S,
  R extends Record<string, (state: S, action: Action<any>) => void | S>
>(options: {
  name: string;
  initialState: S;
  reducers: R;
}): Slice<S, R> {
  const actions = {} as any;
  for (const key of Object.keys(options.reducers)) {
    actions[key] = (payload: any) => ({
      type: `${options.name}/${key}`,
      payload,
    });
  }

  const reducer = (state: S | undefined = options.initialState, action: Action<any>): S => {
    if (!action || !action.type) {
      return state === undefined ? options.initialState : state;
    }
    if (action.type.startsWith(`${options.name}/`)) {
      const key = action.type.substring(options.name.length + 1);
      const handler = options.reducers[key];
      if (handler) {
        // Simple clone to trigger React state updates cleanly
        const nextState = JSON.parse(JSON.stringify(state));
        const result = handler(nextState, action);
        return result !== undefined ? result : nextState;
      }
    }
    return state === undefined ? options.initialState : state;
  };

  return {
    name: options.name,
    reducer,
    actions,
    initialState: options.initialState,
  };
}

export function configureStore<T extends Record<string, Reducer<any, any>>>(options: {
  reducer: T;
}) {
  type State = { [K in keyof T]: T[K] extends Reducer<infer S, any> ? S : never };
  
  const currentState = {} as any;
  // Initialize initial state
  for (const key of Object.keys(options.reducer)) {
    currentState[key] = options.reducer[key](undefined, { type: "@@INIT" });
  }

  const listeners = new Set<() => void>();

  const getState = () => currentState as State;

  const dispatch = (action: Action<any>) => {
    if (!action || !action.type) {
      console.warn("Dispatched undefined or invalid action:", action);
      return action;
    }
    let changed = false;
    const nextState = {} as any;
    for (const key of Object.keys(options.reducer)) {
      const prevSliceState = currentState[key];
      const nextSliceState = options.reducer[key](prevSliceState, action);
      nextState[key] = nextSliceState;
      if (nextSliceState !== prevSliceState) {
        changed = true;
      }
    }
    if (changed) {
      for (const key of Object.keys(nextState)) {
        currentState[key] = nextState[key];
      }
      listeners.forEach((listener) => listener());
    }
    return action;
  };

  const subscribe = (listener: () => void) => {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  };

  return {
    getState,
    dispatch,
    subscribe,
  };
}

const StoreContext = createContext<any>(null);

export function Provider({ store, children }: { store: any; children: React.ReactNode }) {
  return React.createElement(StoreContext.Provider, { value: store }, children);
}

export function useDispatch() {
  const store = useContext(StoreContext);
  if (!store) throw new Error("useDispatch must be used within a Redux Provider");
  return store.dispatch;
}

export function useSelector<TState, TSelected>(
  selector: (state: TState) => TSelected
): TSelected {
  const store = useContext(StoreContext);
  if (!store) throw new Error("useSelector must be used within a Redux Provider");

  const [selectedState, setSelectedState] = useState(() => selector(store.getState()));

  useEffect(() => {
    let active = true;
    const unsubscribe = store.subscribe(() => {
      if (!active) return;
      const nextSelected = selector(store.getState());
      setSelectedState((prev) => {
        if (JSON.stringify(prev) === JSON.stringify(nextSelected)) {
          return prev;
        }
        return nextSelected;
      });
    });
    return () => {
      active = false;
      unsubscribe();
    };
  }, [store, selector]);

  return selectedState;
}

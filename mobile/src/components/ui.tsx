import React, { ReactNode, useMemo } from "react";
import { FlatList, Modal, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import { AppTheme, theme, useTheme } from "../theme";

const touchHitSlop = { top: 8, right: 8, bottom: 8, left: 8 };

function useThemedStyles() {
  const activeTheme = useTheme();
  return useMemo(() => createUiStyles(activeTheme), [activeTheme]);
}

function isLightPalette(theme: AppTheme) {
  return theme.colors.bg.toLowerCase().startsWith("#f") || theme.colors.bg.toLowerCase().startsWith("#e");
}

export function Screen({ children }: { children: ReactNode }) {
  const styles = useThemedStyles();
  return <View style={styles.screen}>{children}</View>;
}

export function Section({
  title,
  subtitle,
  right,
  children,
}: {
  title: string;
  subtitle?: string;
  right?: ReactNode;
  children: ReactNode;
}) {
  const styles = useThemedStyles();
  return (
    <View style={styles.section}>
      <View style={styles.sectionHeader}>
        <View style={styles.sectionCopy}>
          <Text accessibilityRole="header" style={styles.sectionTitle}>{title}</Text>
          {subtitle ? <Text style={styles.sectionSubtitle}>{subtitle}</Text> : null}
        </View>
        {right}
      </View>
      {children}
    </View>
  );
}

export function Surface({ children, tone = "default" }: { children: ReactNode; tone?: "default" | "player" | "owner" }) {
  const styles = useThemedStyles();
  const toneStyle =
    tone === "player" ? styles.surfacePlayer : tone === "owner" ? styles.surfaceOwner : styles.surfaceDefault;
  return <View style={[styles.surface, toneStyle]}>{children}</View>;
}

export function AppButton({
  label,
  onPress,
  tone = "primary",
  disabled = false,
  accessibilityLabel,
  accessibilityHint,
}: {
  label: string;
  onPress: () => void;
  tone?: "primary" | "secondary" | "owner" | "danger";
  disabled?: boolean;
  accessibilityLabel?: string;
  accessibilityHint?: string;
}) {
  const styles = useThemedStyles();
  const activeTheme = useTheme();
  const toneStyle =
    tone === "secondary"
      ? styles.buttonSecondary
      : tone === "owner"
        ? styles.buttonOwner
        : tone === "danger"
          ? styles.buttonDanger
          : styles.buttonPrimary;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={accessibilityLabel || label}
      accessibilityHint={accessibilityHint}
      accessibilityState={{ disabled }}
      hitSlop={touchHitSlop}
      style={[styles.button, toneStyle, disabled && styles.buttonDisabled]}
      onPress={onPress}
      disabled={disabled}
    >
      <Text style={[styles.buttonText, tone === "secondary" && { color: activeTheme.colors.text }]}>{label}</Text>
    </Pressable>
  );
}

export function Chip({
  label,
  active = false,
  onPress,
  tone = "default",
}: {
  label: string;
  active?: boolean;
  onPress?: () => void;
  tone?: "default" | "player" | "owner";
}) {
  const styles = useThemedStyles();
  const activeStyle =
    tone === "player"
      ? styles.chipPlayer
      : tone === "owner"
        ? styles.chipOwner
        : styles.chipActive;
  const content = (
    <View style={[styles.chip, active && activeStyle]}>
      <Text style={[styles.chipText, active && styles.chipTextActive]}>{label}</Text>
    </View>
  );
  return onPress ? (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ selected: active }}
      hitSlop={touchHitSlop}
      onPress={onPress}
    >
      {content}
    </Pressable>
  ) : content;
}

export function SegmentedTabs<T extends string>({
  value,
  items,
  onChange,
  tone = "player",
}: {
  value: T;
  items: Array<{ key: T; label: string }>;
  onChange: (value: T) => void;
  tone?: "player" | "owner";
}) {
  const styles = useThemedStyles();
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.tabRow}>
      {items.map((item) => (
        <Chip
          key={item.key}
          label={item.label}
          active={item.key === value}
          onPress={() => onChange(item.key)}
          tone={tone}
        />
      ))}
    </ScrollView>
  );
}

export function TabGrid<T extends string>({
  value,
  items,
  onChange,
  tone = "player",
  columns = 3,
}: {
  value: T;
  items: Array<{ key: T; label: string; icon?: string }>;
  onChange: (value: T) => void;
  tone?: "player" | "owner";
  columns?: number;
}) {
  const styles = useThemedStyles();
  void columns;
  return (
    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.mobileTabRail}>
      {items.map((item) => {
        const active = item.key === value;
        const toneStyle = tone === "owner" ? styles.mobileTabOwner : styles.mobileTabPlayer;
        return (
          <Pressable
            key={item.key}
            accessibilityRole="tab"
            accessibilityLabel={item.label}
            accessibilityState={{ selected: active }}
            hitSlop={touchHitSlop}
            onPress={() => onChange(item.key)}
            style={[styles.mobileTabItem, active && toneStyle]}
          >
            <Text style={[styles.mobileTabIcon, active && styles.mobileTabTextActive]}>
              {item.icon || item.label.slice(0, 2)}
            </Text>
            <Text style={[styles.mobileTabText, active && styles.mobileTabTextActive]}>{item.label}</Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}

export type MobileNavItem<T extends string = string> = {
  key: T;
  label: string;
  icon?: string;
  hint?: string;
};

export type MobileFeatureGroup<T extends string = string> = {
  title: string;
  subtitle?: string;
  items: MobileNavItem<T>[];
};

export function BottomNavigation<T extends string>({
  value,
  items,
  onChange,
  onMore,
  moreLabel = "More",
  tone = "player",
}: {
  value: T;
  items: MobileNavItem<T>[];
  onChange: (value: T) => void;
  onMore: () => void;
  moreLabel?: string;
  tone?: "player" | "owner";
}) {
  const styles = useThemedStyles();
  const activeTheme = useTheme();
  const accent = tone === "owner" ? activeTheme.colors.owner : activeTheme.colors.player;

  return (
    <View style={styles.bottomNavWrap}>
      <View style={styles.bottomNav}>
        {items.map((item) => {
          const active = item.key === value;
          return (
            <Pressable
              key={item.key}
              accessibilityRole="tab"
              accessibilityState={{ selected: active }}
              accessibilityLabel={item.label}
              accessibilityHint={item.hint || `Open ${item.label}`}
              hitSlop={touchHitSlop}
              onPress={() => onChange(item.key)}
              style={styles.bottomNavItem}
            >
              <View style={[styles.bottomNavIcon, active && { backgroundColor: accent }]}>
                <Text style={[styles.bottomNavIconText, active && styles.bottomNavIconTextActive]}>
                  {item.icon || item.label.slice(0, 2)}
                </Text>
              </View>
              <Text style={[styles.bottomNavText, active && { color: activeTheme.colors.text }]} numberOfLines={1}>
                {item.label}
              </Text>
            </Pressable>
          );
        })}
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={moreLabel}
          accessibilityHint="Open all available mobile features"
          hitSlop={touchHitSlop}
          onPress={onMore}
          style={styles.bottomNavItem}
        >
          <View style={styles.bottomNavIcon}>
            <Text style={styles.bottomNavIconText}>...</Text>
          </View>
          <Text style={styles.bottomNavText} numberOfLines={1}>{moreLabel}</Text>
        </Pressable>
      </View>
    </View>
  );
}

export function QuickActionRail<T extends string>({
  title,
  subtitle,
  actions,
  onSelect,
  tone = "player",
}: {
  title: string;
  subtitle?: string;
  actions: MobileNavItem<T>[];
  onSelect: (value: T) => void;
  tone?: "player" | "owner";
}) {
  const styles = useThemedStyles();
  const activeTheme = useTheme();
  const accent = tone === "owner" ? activeTheme.colors.owner : activeTheme.colors.player;

  return (
    <View style={styles.quickActionPanel}>
      <View style={styles.sectionHeader}>
        <View style={styles.sectionCopy}>
          <Text style={styles.quickActionTitle}>{title}</Text>
          {subtitle ? <Text style={styles.sectionSubtitle}>{subtitle}</Text> : null}
        </View>
      </View>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.quickActionRow}>
        {actions.map((item) => (
          <Pressable
            key={item.key}
            accessibilityRole="button"
            accessibilityLabel={item.label}
            accessibilityHint={item.hint || `Open ${item.label}`}
            hitSlop={touchHitSlop}
            onPress={() => onSelect(item.key)}
            style={styles.quickActionCard}
          >
            <View style={[styles.quickActionIcon, { backgroundColor: accent }]}>
              <Text style={styles.quickActionIconText}>{item.icon || item.label.slice(0, 2)}</Text>
            </View>
            <Text style={styles.quickActionLabel}>{item.label}</Text>
            {item.hint ? <Text style={styles.quickActionHint}>{item.hint}</Text> : null}
          </Pressable>
        ))}
      </ScrollView>
    </View>
  );
}

export function FeatureLauncher<T extends string>({
  visible,
  title,
  subtitle,
  groups,
  onSelect,
  onClose,
}: {
  visible: boolean;
  title: string;
  subtitle?: string;
  groups: MobileFeatureGroup<T>[];
  onSelect: (value: T) => void;
  onClose: () => void;
}) {
  const styles = useThemedStyles();

  return (
    <SheetModal visible={visible} title={title} subtitle={subtitle} onClose={onClose}>
      <ScrollView style={styles.featureSheet} contentContainerStyle={styles.featureSheetContent}>
        {groups.map((group) => (
          <View key={group.title} style={styles.featureGroup}>
            <Text style={styles.featureGroupTitle}>{group.title}</Text>
            {group.subtitle ? <Text style={styles.sectionSubtitle}>{group.subtitle}</Text> : null}
            <View style={styles.featureGrid}>
              {group.items.map((item) => (
                <Pressable
                  key={item.key}
                  accessibilityRole="button"
                  accessibilityLabel={item.label}
                  accessibilityHint={item.hint || `Open ${item.label}`}
                  hitSlop={touchHitSlop}
                  onPress={() => {
                    onSelect(item.key);
                    onClose();
                  }}
                  style={styles.featureTile}
                >
                  <Text style={styles.featureTileIcon}>{item.icon || item.label.slice(0, 2)}</Text>
                  <Text style={styles.featureTileTitle}>{item.label}</Text>
                  {item.hint ? <Text style={styles.featureTileHint}>{item.hint}</Text> : null}
                </Pressable>
              ))}
            </View>
          </View>
        ))}
      </ScrollView>
    </SheetModal>
  );
}

export function Metric({
  label,
  value,
  tone = "default",
}: {
  label: string;
  value: string | number;
  tone?: "default" | "player" | "owner";
}) {
  const styles = useThemedStyles();
  const accent = tone === "owner" ? styles.metricOwner : tone === "player" ? styles.metricPlayer : undefined;
  return (
    <View style={[styles.metric, accent]}>
      <Text style={styles.metricLabel}>{label}</Text>
      <Text style={styles.metricValue}>{value}</Text>
    </View>
  );
}

export function LabeledInput({
  label,
  value,
  onChangeText,
  placeholder,
  keyboardType,
  secureTextEntry,
  multiline = false,
  editable = true,
}: {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  placeholder?: string;
  keyboardType?: "default" | "email-address" | "numeric";
  secureTextEntry?: boolean;
  multiline?: boolean;
  editable?: boolean;
}) {
  const styles = useThemedStyles();
  const activeTheme = useTheme();
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput
        accessibilityLabel={label}
        accessibilityHint={placeholder}
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor={activeTheme.colors.muted}
        keyboardType={keyboardType}
        secureTextEntry={secureTextEntry}
        multiline={multiline}
        editable={editable}
        style={[styles.input, multiline && styles.inputMultiline, !editable && styles.inputReadonly]}
      />
    </View>
  );
}

export function SelectField({
  label,
  value,
  onPress,
  placeholder,
}: {
  label: string;
  value?: string;
  onPress: () => void;
  placeholder?: string;
}) {
  const styles = useThemedStyles();
  return (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={label}
        accessibilityHint={placeholder || "Open selector"}
        hitSlop={touchHitSlop}
        onPress={onPress}
        style={styles.selectField}
      >
        <Text style={value ? styles.selectFieldText : styles.selectFieldPlaceholder}>{value || placeholder || "Select"}</Text>
        <Text style={styles.selectFieldChevron}>v</Text>
      </Pressable>
    </View>
  );
}

export function EmptyState({ text }: { text: string }) {
  const styles = useThemedStyles();
  return (
    <View style={styles.emptyState}>
      <Text style={styles.emptyText}>{text}</Text>
    </View>
  );
}

export function FieldPair({ left, right }: { left: ReactNode; right: ReactNode }) {
  const styles = useThemedStyles();
  return <View style={styles.fieldPair}>{left}{right}</View>;
}

export function SheetModal({
  visible,
  title,
  subtitle,
  children,
  onClose,
}: {
  visible: boolean;
  title: string;
  subtitle?: string;
  children: ReactNode;
  onClose: () => void;
}) {
  const styles = useThemedStyles();
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <View style={styles.modalBackdrop}>
        <Pressable style={StyleSheet.absoluteFillObject} onPress={onClose} />
        <View style={styles.modalSheet}>
          <View style={styles.modalHandle} />
          <View style={styles.modalHeader}>
            <View style={styles.sectionCopy}>
              <Text style={styles.sectionTitle}>{title}</Text>
              {subtitle ? <Text style={styles.sectionSubtitle}>{subtitle}</Text> : null}
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Close"
              accessibilityHint={`Close ${title}`}
              hitSlop={touchHitSlop}
              onPress={onClose}
            >
              <Text style={styles.modalClose}>Close</Text>
            </Pressable>
          </View>
          {children}
        </View>
      </View>
    </Modal>
  );
}

export function OptionPicker<T extends string | number>({
  visible,
  title,
  subtitle,
  options,
  onClose,
  onSelect,
}: {
  visible: boolean;
  title: string;
  subtitle?: string;
  options: Array<{ label: string; value: T; hint?: string }>;
  onClose: () => void;
  onSelect: (value: T) => void;
}) {
  const styles = useThemedStyles();
  return (
    <SheetModal visible={visible} title={title} subtitle={subtitle} onClose={onClose}>
      <FlatList
        data={options}
        keyExtractor={(item) => String(item.value)}
        renderItem={({ item }) => (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel={item.label}
            accessibilityHint={item.hint || `Choose ${item.label}`}
            hitSlop={touchHitSlop}
            onPress={() => {
              onSelect(item.value);
              onClose();
            }}
            style={styles.optionRow}
          >
            <View style={styles.sectionCopy}>
              <Text style={styles.optionTitle}>{item.label}</Text>
              {item.hint ? <Text style={styles.optionHint}>{item.hint}</Text> : null}
            </View>
            <Text style={styles.selectFieldChevron}>&gt;</Text>
          </Pressable>
        )}
      />
    </SheetModal>
  );
}

export function ConfirmDialog({
  visible,
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  tone = "primary",
  onConfirm,
  onCancel,
}: {
  visible: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: "primary" | "owner" | "danger";
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const styles = useThemedStyles();
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onCancel}>
      <View style={styles.modalBackdrop}>
        <View style={styles.dialog}>
          <Text style={styles.dialogTitle}>{title}</Text>
          <Text style={styles.dialogMessage}>{message}</Text>
          <View style={styles.dialogActions}>
            <AppButton label={cancelLabel} tone="secondary" onPress={onCancel} />
            <AppButton label={confirmLabel} tone={tone} onPress={onConfirm} />
          </View>
        </View>
      </View>
    </Modal>
  );
}

export function ProfileHero({
  initials,
  name,
  subtitle,
  meta,
}: {
  initials: string;
  name: string;
  subtitle: string;
  meta?: string;
}) {
  const styles = useThemedStyles();
  return (
    <View style={styles.profileHero}>
      <View style={styles.profileAvatar}>
        <Text style={styles.profileAvatarText}>{initials}</Text>
      </View>
      <View style={styles.sectionCopy}>
        <Text style={styles.profileName}>{name}</Text>
        <Text style={styles.sectionSubtitle}>{subtitle}</Text>
        {meta ? <Text style={styles.profileMeta}>{meta}</Text> : null}
      </View>
    </View>
  );
}

function createUiStyles(theme: AppTheme) {
  const light = isLightPalette(theme);
  return StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: theme.colors.bg,
  },
  section: {
    gap: theme.spacing.sm,
  },
  sectionHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    gap: theme.spacing.md,
    alignItems: "center",
  },
  sectionCopy: {
    flex: 1,
    gap: 4,
  },
  sectionTitle: {
    color: theme.colors.text,
    fontSize: 20,
    fontWeight: "800",
  },
  sectionSubtitle: {
    color: theme.colors.muted,
    fontSize: 13,
    lineHeight: 18,
  },
  surface: {
    borderRadius: theme.radius.lg,
    borderWidth: 1,
    padding: theme.spacing.md,
    gap: theme.spacing.sm,
  },
  surfaceDefault: {
    backgroundColor: theme.colors.card,
    borderColor: theme.colors.border,
  },
  surfacePlayer: {
    backgroundColor: theme.colors.card,
    borderColor: theme.colors.player,
  },
  surfaceOwner: {
    backgroundColor: theme.colors.card,
    borderColor: theme.colors.owner,
  },
  button: {
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",
    minHeight: 44,
  },
  buttonPrimary: {
    backgroundColor: theme.colors.player,
  },
  buttonSecondary: {
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: "transparent",
  },
  buttonOwner: {
    backgroundColor: theme.colors.owner,
  },
  buttonDanger: {
    backgroundColor: theme.colors.danger,
  },
  buttonDisabled: {
    opacity: 0.5,
  },
  buttonText: {
    color: "#ffffff",
    fontWeight: "800",
    fontSize: 14,
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 9,
    minHeight: 44,
    borderRadius: theme.radius.pill,
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: theme.colors.panelSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  chipPlayer: {
    backgroundColor: theme.colors.playerSoft,
    borderColor: theme.colors.player,
  },
  chipOwner: {
    backgroundColor: theme.colors.ownerSoft,
    borderColor: theme.colors.owner,
  },
  chipActive: {
    backgroundColor: theme.colors.panel,
  },
  chipText: {
    color: theme.colors.muted,
    fontWeight: "700",
    textTransform: "capitalize",
  },
  chipTextActive: {
    color: theme.colors.text,
  },
  tabRow: {
    gap: theme.spacing.xs,
    paddingVertical: 2,
  },
  mobileTabRail: {
    gap: 10,
    paddingVertical: 4,
    paddingHorizontal: 2,
  },
  mobileTabItem: {
    minWidth: 86,
    minHeight: 58,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: theme.colors.panelSoft,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 12,
    paddingVertical: 9,
    gap: 2,
  },
  mobileTabPlayer: {
    borderColor: theme.colors.player,
    backgroundColor: theme.colors.playerSoft,
  },
  mobileTabOwner: {
    borderColor: theme.colors.owner,
    backgroundColor: theme.colors.ownerSoft,
  },
  mobileTabIcon: {
    color: theme.colors.muted,
    fontSize: 11,
    fontWeight: "900",
    textTransform: "uppercase",
  },
  mobileTabText: {
    color: theme.colors.muted,
    fontSize: 12,
    fontWeight: "800",
  },
  mobileTabTextActive: {
    color: theme.colors.text,
  },
  bottomNavWrap: {
    position: "absolute",
    left: 12,
    right: 12,
    bottom: 10,
    zIndex: 30,
  },
  bottomNav: {
    minHeight: 70,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: light ? "rgba(255, 255, 255, 0.96)" : "rgba(10, 20, 34, 0.96)",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
    paddingHorizontal: 8,
    paddingVertical: 8,
    shadowColor: "#000000",
    shadowOpacity: light ? 0.12 : 0.34,
    shadowRadius: 18,
    shadowOffset: { width: 0, height: 8 },
    elevation: 8,
  },
  bottomNavItem: {
    flex: 1,
    minHeight: 54,
    alignItems: "center",
    justifyContent: "center",
    gap: 4,
  },
  bottomNavIcon: {
    width: 34,
    height: 30,
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: theme.colors.panelSoft,
  },
  bottomNavIconText: {
    color: theme.colors.muted,
    fontSize: 11,
    fontWeight: "900",
    textTransform: "uppercase",
  },
  bottomNavIconTextActive: {
    color: "#ffffff",
  },
  bottomNavText: {
    color: theme.colors.muted,
    fontSize: 11,
    fontWeight: "800",
    maxWidth: 70,
  },
  quickActionPanel: {
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: theme.colors.card,
    borderRadius: theme.radius.lg,
    padding: theme.spacing.md,
    gap: theme.spacing.sm,
  },
  quickActionTitle: {
    color: theme.colors.text,
    fontSize: 16,
    fontWeight: "900",
  },
  quickActionRow: {
    gap: theme.spacing.sm,
    paddingRight: theme.spacing.sm,
  },
  quickActionCard: {
    width: 142,
    minHeight: 118,
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: theme.colors.panelSoft,
    borderRadius: theme.radius.md,
    padding: theme.spacing.sm,
    gap: 8,
  },
  quickActionIcon: {
    width: 34,
    height: 34,
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",
  },
  quickActionIconText: {
    color: "#ffffff",
    fontSize: 11,
    fontWeight: "900",
    textTransform: "uppercase",
  },
  quickActionLabel: {
    color: theme.colors.text,
    fontSize: 14,
    fontWeight: "900",
  },
  quickActionHint: {
    color: theme.colors.muted,
    fontSize: 12,
    lineHeight: 16,
  },
  tabGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    marginHorizontal: -4,
  },
  tabGridItem: {
    paddingHorizontal: 4,
    paddingVertical: 4,
  },
  tabGridPlayer: {
    borderRadius: 18,
    backgroundColor: theme.colors.playerSoft,
    borderColor: theme.colors.player,
  },
  tabGridOwner: {
    borderRadius: 18,
    backgroundColor: theme.colors.ownerSoft,
    borderColor: theme.colors.owner,
  },
  tabGridText: {
    textAlign: "center",
    color: theme.colors.muted,
    fontWeight: "800",
    backgroundColor: theme.colors.panelSoft,
    borderColor: theme.colors.border,
    borderWidth: 1,
    overflow: "hidden",
    borderRadius: 18,
    paddingVertical: 14,
    paddingHorizontal: 8,
  },
  tabGridTextActive: {
    color: theme.colors.text,
  },
  metric: {
    flex: 1,
    minWidth: 110,
    backgroundColor: theme.colors.panelSoft,
    borderRadius: theme.radius.md,
    borderWidth: 1,
    borderColor: theme.colors.border,
    padding: theme.spacing.sm,
    gap: 6,
  },
  metricPlayer: {
    borderColor: theme.colors.player,
  },
  metricOwner: {
    borderColor: theme.colors.owner,
  },
  metricLabel: {
    color: theme.colors.muted,
    fontSize: 12,
    fontWeight: "700",
    textTransform: "uppercase",
  },
  metricValue: {
    color: theme.colors.text,
    fontSize: 22,
    fontWeight: "800",
  },
  field: {
    gap: 6,
    flex: 1,
  },
  fieldLabel: {
    color: theme.colors.text,
    fontSize: 13,
    fontWeight: "800",
  },
  input: {
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: theme.colors.input,
    color: theme.colors.text,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 12,
    minHeight: 48,
  },
  inputMultiline: {
    minHeight: 88,
    textAlignVertical: "top",
  },
  inputReadonly: {
    opacity: 0.9,
  },
  selectField: {
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: theme.colors.input,
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 14,
    minHeight: 48,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 8,
  },
  selectFieldText: {
    color: theme.colors.text,
    fontSize: 16,
    fontWeight: "700",
    flex: 1,
  },
  selectFieldPlaceholder: {
    color: theme.colors.muted,
    fontSize: 16,
    flex: 1,
  },
  selectFieldChevron: {
    color: theme.colors.muted,
    fontSize: 18,
    fontWeight: "900",
  },
  emptyState: {
    borderWidth: 1,
    borderStyle: "dashed",
    borderColor: theme.colors.border,
    borderRadius: theme.radius.md,
    padding: theme.spacing.lg,
  },
  emptyText: {
    color: theme.colors.muted,
    textAlign: "center",
    lineHeight: 20,
  },
  fieldPair: {
    flexDirection: "row",
    gap: theme.spacing.sm,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: light ? "rgba(15, 23, 42, 0.28)" : "rgba(2, 6, 18, 0.72)",
    justifyContent: "flex-end",
    padding: 16,
  },
  modalSheet: {
    backgroundColor: theme.colors.bgElevated,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: theme.colors.border,
    maxHeight: "72%",
    overflow: "hidden",
    shadowColor: "#000000",
    shadowOpacity: light ? 0.12 : 0.32,
    shadowRadius: 28,
    shadowOffset: { width: 0, height: -8 },
  },
  modalHandle: {
    alignSelf: "center",
    width: 54,
    height: 5,
    borderRadius: 999,
    backgroundColor: theme.colors.border,
    marginTop: 10,
    marginBottom: 8,
  },
  modalHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
    paddingHorizontal: 18,
    paddingBottom: 14,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
  },
  modalClose: {
    color: theme.colors.text,
    fontWeight: "700",
  },
  optionRow: {
    paddingHorizontal: 18,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: theme.colors.border,
    backgroundColor: theme.colors.bgElevated,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
  },
  optionTitle: {
    color: theme.colors.text,
    fontWeight: "800",
    fontSize: 16,
  },
  optionHint: {
    color: theme.colors.muted,
    fontSize: 13,
    marginTop: 2,
  },
  featureSheet: {
    maxHeight: 520,
  },
  featureSheetContent: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    gap: 18,
  },
  featureGroup: {
    gap: 10,
  },
  featureGroupTitle: {
    color: theme.colors.text,
    fontSize: 15,
    fontWeight: "900",
  },
  featureGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 10,
  },
  featureTile: {
    width: "47%",
    minHeight: 104,
    borderRadius: theme.radius.md,
    borderWidth: 1,
    borderColor: theme.colors.border,
    backgroundColor: theme.colors.panelSoft,
    padding: theme.spacing.sm,
    gap: 6,
  },
  featureTileIcon: {
    color: theme.colors.text,
    fontSize: 11,
    fontWeight: "900",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  featureTileTitle: {
    color: theme.colors.text,
    fontWeight: "900",
    fontSize: 14,
  },
  featureTileHint: {
    color: theme.colors.muted,
    fontSize: 12,
    lineHeight: 16,
  },
  dialog: {
    backgroundColor: theme.colors.bgElevated,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: theme.colors.border,
    padding: 20,
    gap: 14,
  },
  dialogTitle: {
    color: theme.colors.text,
    fontSize: 20,
    fontWeight: "800",
  },
  dialogMessage: {
    color: theme.colors.muted,
    lineHeight: 21,
  },
  dialogActions: {
    flexDirection: "row",
    gap: 10,
  },
  profileHero: {
    flexDirection: "row",
    alignItems: "center",
    gap: 14,
  },
  profileAvatar: {
    width: 62,
    height: 62,
    borderRadius: 20,
    backgroundColor: theme.colors.playerSoft,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: theme.colors.player,
  },
  profileAvatarText: {
    color: theme.colors.text,
    fontSize: 22,
    fontWeight: "900",
  },
  profileName: {
    color: theme.colors.text,
    fontSize: 22,
    fontWeight: "800",
  },
  profileMeta: {
    color: theme.colors.muted,
    fontSize: 13,
  },
  });
}

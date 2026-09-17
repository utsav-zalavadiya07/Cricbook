import LoginPageClient from "./LoginPageClient";
import { Suspense } from "react";

export const metadata = { title: "Login | Cricbook" };

export default function LoginPage() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <LoginPageClient />
    </Suspense>
  );
}

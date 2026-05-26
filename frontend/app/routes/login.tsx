import type { Route } from "./+types/login";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router";
import { useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { WhopLogo } from "../components/whop-logo";
import {
  ApiRequestError,
  getMe,
  login,
  signup,
  type AuthPayload,
  type MeResponse,
} from "../lib/auth-api";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Log in - Whop Task Marketplace" },
    { name: "description", content: "Log in or create your account" },
  ];
}

export default function Login() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [signupForm, setSignupForm] = useState<AuthPayload>({
    username: "",
    password: "",
  });
  const [loginForm, setLoginForm] = useState<AuthPayload>({
    username: "",
    password: "",
  });

  const meQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: getMe,
  });

  useEffect(() => {
    if (meQuery.data) {
      navigate("/");
    }
  }, [meQuery.data, navigate]);

  const onAuthSuccess = (user: MeResponse) => {
    queryClient.setQueryData(["auth", "me"], user);
    navigate("/");
  };

  const signupMutation = useMutation({
    mutationFn: (payload: AuthPayload) => signup(payload),
    onSuccess: onAuthSuccess,
  });

  const loginMutation = useMutation({
    mutationFn: (payload: AuthPayload) => login(payload),
    onSuccess: onAuthSuccess,
  });

  const formError = useMemo(() => {
    const candidate = signupMutation.error ?? loginMutation.error;
    if (candidate instanceof ApiRequestError) {
      return candidate.message;
    }
    if (candidate) {
      return "Unable to authenticate";
    }
    return null;
  }, [loginMutation.error, signupMutation.error]);

  const onSignup = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    signupMutation.reset();
    loginMutation.reset();
    signupMutation.mutate({
      username: signupForm.username.trim().toLowerCase(),
      password: signupForm.password,
    });
  };

  const onLogin = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    signupMutation.reset();
    loginMutation.reset();
    loginMutation.mutate({
      username: loginForm.username.trim().toLowerCase(),
      password: loginForm.password,
    });
  };

  return (
    <main className="min-h-screen bg-[#f8f7f4] text-gray-950">
      <header className="border-b border-[#ece8e3] bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-4">
          <Link className="text-sm font-medium text-gray-600" to="/">
            Back to marketplace
          </Link>
          <WhopLogo className="h-[22px] w-auto text-[#1f1f1f]" />
        </div>
      </header>

      <div className="mx-auto grid max-w-4xl gap-6 px-4 py-10 md:grid-cols-2">
        <section className="space-y-4 rounded-lg border border-[#ece8e3] bg-white p-5">
          <h1 className="text-xl font-semibold text-gray-900">Create account</h1>
          <form className="space-y-3" onSubmit={onSignup}>
            <label className="block space-y-1">
              <span className="block text-sm font-medium text-gray-700">Username</span>
              <input
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                name="signup-username"
                value={signupForm.username}
                onChange={(event) =>
                  setSignupForm((prev) => ({ ...prev, username: event.target.value }))
                }
                required
                minLength={3}
                maxLength={30}
                pattern="^[a-z0-9_]+$"
              />
            </label>
            <label className="block space-y-1">
              <span className="block text-sm font-medium text-gray-700">Password</span>
              <input
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                type="password"
                name="signup-password"
                value={signupForm.password}
                onChange={(event) =>
                  setSignupForm((prev) => ({ ...prev, password: event.target.value }))
                }
                required
                minLength={8}
              />
            </label>
            <button
              className="w-full rounded-md bg-[#fa4616] px-4 py-2 text-sm font-semibold text-white shadow-[0_6px_18px_rgba(250,70,22,0.28)] disabled:opacity-60"
              type="submit"
              disabled={signupMutation.isPending}
            >
              {signupMutation.isPending ? "Creating..." : "Sign up"}
            </button>
          </form>
        </section>

        <section className="space-y-4 rounded-lg border border-[#ece8e3] bg-white p-5">
          <h2 className="text-xl font-semibold text-gray-900">Log in</h2>
          <form className="space-y-3" onSubmit={onLogin}>
            <label className="block space-y-1">
              <span className="block text-sm font-medium text-gray-700">Username</span>
              <input
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                name="login-username"
                value={loginForm.username}
                onChange={(event) =>
                  setLoginForm((prev) => ({ ...prev, username: event.target.value }))
                }
                required
                minLength={3}
                maxLength={30}
                pattern="^[a-z0-9_]+$"
              />
            </label>
            <label className="block space-y-1">
              <span className="block text-sm font-medium text-gray-700">Password</span>
              <input
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                type="password"
                name="login-password"
                value={loginForm.password}
                onChange={(event) =>
                  setLoginForm((prev) => ({ ...prev, password: event.target.value }))
                }
                required
                minLength={8}
              />
            </label>
            <button
              className="w-full rounded-md border border-[#fac7b8] bg-[#fff2ed] px-4 py-2 text-sm font-semibold text-[#ba330f] disabled:opacity-60"
              type="submit"
              disabled={loginMutation.isPending}
            >
              {loginMutation.isPending ? "Logging in..." : "Log in"}
            </button>
          </form>
          {formError && <p className="text-sm text-red-600">{formError}</p>}
        </section>
      </div>
    </main>
  );
}

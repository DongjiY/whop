import type { Route } from "./+types/home";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate } from "react-router";
import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import { ApiRequestError } from "../lib/api-client";
import { WhopLogo } from "../components/whop-logo";
import { getMe, logout } from "../lib/auth-api";
import { listMyJobs } from "../lib/jobs-api";
import {
  acceptOffer,
  createOffer,
  listOffers,
  withdrawOffer,
  type CreateOfferPayload,
  type Offer,
} from "../lib/offer-api";
import {
  createTask as createTaskApi,
  getTask,
  listTasks,
  type CreateTaskPayload,
  type Task,
} from "../lib/task-api";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Whop Task Marketplace" },
    { name: "description", content: "Browse and create marketplace tasks" },
  ];
}

export default function Home() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null);
  const [taskForm, setTaskForm] = useState({
    title: "",
    description: "",
    budgetAmount: "",
  });
  const [offerForm, setOfferForm] = useState({
    amount: "",
    message: "",
  });

  const meQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: getMe,
    staleTime: 0,
    refetchOnMount: "always",
  });

  const tasksQuery = useQuery({
    queryKey: ["tasks"],
    queryFn: listTasks,
  });

  const activeTaskId = selectedTaskId ?? tasksQuery.data?.[0]?.id ?? null;

  const taskDetailQuery = useQuery({
    queryKey: ["tasks", activeTaskId],
    queryFn: () => {
      if (!activeTaskId) {
        throw new Error("No task selected");
      }
      return getTask(activeTaskId);
    },
    enabled: Boolean(activeTaskId),
  });

  const offersQuery = useQuery({
    queryKey: ["offers", activeTaskId],
    queryFn: () => {
      if (!activeTaskId) {
        throw new Error("No task selected");
      }
      return listOffers(activeTaskId);
    },
    enabled: Boolean(activeTaskId && meQuery.data),
  });

  const jobsQuery = useQuery({
    queryKey: ["jobs"],
    queryFn: listMyJobs,
    enabled: Boolean(meQuery.data),
  });

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: async () => {
      await queryClient.cancelQueries({ queryKey: ["auth", "me"] });
      queryClient.setQueryData(["auth", "me"], null);
      queryClient.removeQueries({ queryKey: ["offers"] });
      queryClient.removeQueries({ queryKey: ["jobs"] });
      navigate("/login");
    },
  });

  const createTaskMutation = useMutation({
    mutationFn: (payload: CreateTaskPayload) => createTaskApi(payload),
    onSuccess: async (task) => {
      setTaskForm({ title: "", description: "", budgetAmount: "" });
      setSelectedTaskId(task.id);
      await queryClient.invalidateQueries({ queryKey: ["tasks"] });
    },
  });

  const createOfferMutation = useMutation({
    mutationFn: ({ taskId, payload }: { taskId: string; payload: CreateOfferPayload }) =>
      createOffer(taskId, payload),
    onSuccess: async () => {
      setOfferForm({ amount: "", message: "" });
      await queryClient.invalidateQueries({ queryKey: ["offers", activeTaskId] });
    },
  });

  const acceptOfferMutation = useMutation({
    mutationFn: ({ taskId, offerId }: { taskId: string; offerId: string }) =>
      acceptOffer(taskId, offerId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["tasks"] });
      await queryClient.invalidateQueries({ queryKey: ["offers", activeTaskId] });
      await queryClient.invalidateQueries({ queryKey: ["jobs"] });
    },
  });

  const withdrawOfferMutation = useMutation({
    mutationFn: ({ taskId, offerId }: { taskId: string; offerId: string }) =>
      withdrawOffer(taskId, offerId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["offers", activeTaskId] });
    },
  });

  const createTaskError = useMemo(() => {
    if (createTaskMutation.error instanceof ApiRequestError) {
      return createTaskMutation.error.message;
    }
    if (createTaskMutation.error) {
      return "Unable to create task";
    }
    return null;
  }, [createTaskMutation.error]);

  const onCreateTask = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    createTaskMutation.mutate({
      title: taskForm.title.trim(),
      description: taskForm.description.trim(),
      budgetAmount: Number(taskForm.budgetAmount),
      budgetCurrency: "USD",
    });
  };

  const createOfferError = useMemo(() => {
    if (createOfferMutation.error instanceof ApiRequestError) {
      return createOfferMutation.error.message;
    }
    if (createOfferMutation.error) {
      return "Unable to create offer";
    }
    return null;
  }, [createOfferMutation.error]);

  const onCreateOffer = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!activeTaskId) {
      return;
    }
    createOfferMutation.mutate({
      taskId: activeTaskId,
      payload: {
        amount: Number(offerForm.amount),
        currency: "USD",
        message: offerForm.message.trim(),
      },
    });
  };

  const currentUser = meQuery.data ?? null;
  const tasks = tasksQuery.data ?? [];
  const jobs = jobsQuery.data ?? [];
  const activeTask = taskDetailQuery.data ?? tasks.find((task) => task.id === activeTaskId);
  const isTaskOwner = Boolean(
    currentUser && activeTask && currentUser.id === activeTask.owner.id,
  );
  const offers = offersQuery.data ?? [];

  return (
    <main className="min-h-screen bg-[#f8f7f4] text-gray-950">
      <header className="border-b border-[#ece8e3] bg-white/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4">
          <div className="space-y-2">
            <WhopLogo className="h-[22px] w-auto text-[#1f1f1f]" />
            <h1 className="text-xl font-semibold tracking-tight">Task Marketplace</h1>
          </div>
          {currentUser ? (
            <div className="flex items-center gap-3">
              <span className="text-sm text-gray-600">Signed in as {currentUser.username}</span>
              <button
                className="rounded-md border border-[#fac7b8] bg-[#fff2ed] px-3 py-2 text-sm font-semibold text-[#ba330f] disabled:opacity-60"
                type="button"
                onClick={() => logoutMutation.mutate()}
                disabled={logoutMutation.isPending}
              >
                {logoutMutation.isPending ? "Logging out..." : "Log out"}
              </button>
              {logoutMutation.error instanceof ApiRequestError && (
                <span className="text-xs text-red-600">{logoutMutation.error.message}</span>
              )}
            </div>
          ) : (
            <Link
              className="rounded-md bg-[#fa4616] px-4 py-2 text-sm font-semibold text-white shadow-[0_6px_18px_rgba(250,70,22,0.28)]"
              to="/login"
            >
              Log in
            </Link>
          )}
        </div>
      </header>

      <div className="mx-auto grid max-w-6xl gap-6 px-4 py-6 lg:grid-cols-[minmax(0,1fr)_360px]">
        <section className="space-y-4">
          <div className="flex items-end justify-between gap-4">
            <div>
              <h2 className="text-lg font-semibold">Open tasks</h2>
              <p className="text-sm text-gray-600">Browse current marketplace work.</p>
            </div>
            {tasksQuery.isFetching && <p className="text-sm text-gray-500">Refreshing...</p>}
          </div>

          {tasksQuery.isLoading ? (
            <p className="rounded-lg border border-[#ece8e3] bg-white p-4 text-sm text-gray-600">
              Loading tasks...
            </p>
          ) : tasks.length === 0 ? (
            <p className="rounded-lg border border-[#ece8e3] bg-white p-4 text-sm text-gray-600">
              No tasks have been posted yet.
            </p>
          ) : (
            <div className="grid gap-3">
              {tasks.map((task) => (
                <TaskListItem
                  key={task.id}
                  task={task}
                  isSelected={task.id === activeTaskId}
                  onSelect={() => setSelectedTaskId(task.id)}
                />
              ))}
            </div>
          )}

          {currentUser && (
            <div className="pt-2">
              <div className="flex items-end justify-between gap-4">
                <div>
                  <h2 className="text-lg font-semibold">My Jobs</h2>
                  <p className="text-sm text-gray-600">
                    Accepted work where you are the buyer or hired seller.
                  </p>
                </div>
                {jobsQuery.isFetching && <p className="text-sm text-gray-500">Refreshing...</p>}
              </div>

              {jobsQuery.isLoading ? (
                <p className="mt-3 rounded-lg border border-[#ece8e3] bg-white p-4 text-sm text-gray-600">
                  Loading jobs...
                </p>
              ) : jobs.length === 0 ? (
                <p className="mt-3 rounded-lg border border-[#ece8e3] bg-white p-4 text-sm text-gray-600">
                  No accepted jobs yet.
                </p>
              ) : (
                <div className="mt-3 grid gap-3">
                  {jobs.map((job) => (
                    <JobListItem key={job.id} task={job} currentUserId={currentUser.id} />
                  ))}
                </div>
              )}
            </div>
          )}
        </section>

        <aside className="space-y-4">
          <section className="rounded-lg border border-[#ece8e3] bg-white p-4">
            <h2 className="text-base font-semibold">Task details</h2>
            {activeTask ? (
              <div className="mt-3 space-y-3">
                <div>
                  <h3 className="font-medium">{activeTask.title}</h3>
                  <p className="mt-1 text-sm text-gray-600">{activeTask.description}</p>
                </div>
                <dl className="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <dt className="text-gray-500">Budget</dt>
                    <dd className="font-medium">
                      {formatMoney(activeTask.budgetAmount, activeTask.budgetCurrency)}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-gray-500">Posted by</dt>
                    <dd className="font-medium">{activeTask.owner.username}</dd>
                  </div>
                </dl>
                {!currentUser ? (
                  <p className="rounded-md border border-[#ece8e3] bg-[#fbfaf8] px-3 py-2 text-sm text-gray-600">
                    Log in to submit or manage offers.
                  </p>
                ) : (
                  <div className="space-y-3 rounded-md border border-[#ece8e3] bg-[#fbfaf8] p-3">
                    <div className="flex items-center justify-between">
                      <h4 className="text-sm font-semibold">
                        {isTaskOwner ? "Offers on this task" : "Your offer"}
                      </h4>
                      {offersQuery.isFetching && (
                        <span className="text-xs text-gray-500">Refreshing...</span>
                      )}
                    </div>

                    {isTaskOwner ? (
                      offers.length === 0 ? (
                        <p className="text-sm text-gray-600">No offers yet.</p>
                      ) : (
                        <div className="space-y-2">
                          {offers.map((offer) => (
                            <OfferRow
                              key={offer.id}
                              offer={offer}
                              isOwnerView
                              currentUserId={currentUser.id}
                              onAccept={() =>
                                activeTaskId &&
                                acceptOfferMutation.mutate({
                                  taskId: activeTaskId,
                                  offerId: offer.id,
                                })
                              }
                              onWithdraw={() => {}}
                              actionPending={
                                acceptOfferMutation.isPending || withdrawOfferMutation.isPending
                              }
                            />
                          ))}
                        </div>
                      )
                    ) : (
                      <div className="space-y-3">
                        {offers.length > 0 ? (
                          <OfferRow
                            offer={offers[0]}
                            isOwnerView={false}
                            currentUserId={currentUser.id}
                            onAccept={() => {}}
                            onWithdraw={() =>
                              activeTaskId &&
                              withdrawOfferMutation.mutate({
                                taskId: activeTaskId,
                                offerId: offers[0].id,
                              })
                            }
                            actionPending={
                              acceptOfferMutation.isPending || withdrawOfferMutation.isPending
                            }
                          />
                        ) : (
                          <form className="space-y-2" onSubmit={onCreateOffer}>
                            <label className="block space-y-1">
                              <span className="block text-sm font-medium text-gray-700">
                                Offer amount
                              </span>
                              <input
                                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                                type="number"
                                min="0.01"
                                step="0.01"
                                value={offerForm.amount}
                                onChange={(event) =>
                                  setOfferForm((prev) => ({ ...prev, amount: event.target.value }))
                                }
                                required
                              />
                            </label>
                            <label className="block space-y-1">
                              <span className="block text-sm font-medium text-gray-700">
                                Message
                              </span>
                              <textarea
                                className="min-h-20 w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                                value={offerForm.message}
                                onChange={(event) =>
                                  setOfferForm((prev) => ({ ...prev, message: event.target.value }))
                                }
                                required
                                minLength={5}
                                maxLength={5000}
                              />
                            </label>
                            <button
                              className="w-full rounded-md bg-[#fa4616] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60"
                              type="submit"
                              disabled={createOfferMutation.isPending}
                            >
                              {createOfferMutation.isPending ? "Submitting..." : "Submit offer"}
                            </button>
                            {createOfferError && (
                              <p className="text-sm text-red-600">{createOfferError}</p>
                            )}
                          </form>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <p className="mt-3 text-sm text-gray-600">Select a task to view details.</p>
            )}
          </section>

          <section className="rounded-lg border border-[#ece8e3] bg-white p-4">
            <h2 className="text-base font-semibold">Create a task</h2>
            {currentUser ? (
              <form className="mt-3 space-y-3" onSubmit={onCreateTask}>
                <label className="block space-y-1">
                  <span className="block text-sm font-medium text-gray-700">Title</span>
                  <input
                    className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                    value={taskForm.title}
                    onChange={(event) =>
                      setTaskForm((prev) => ({ ...prev, title: event.target.value }))
                    }
                    required
                    minLength={3}
                    maxLength={120}
                  />
                </label>
                <label className="block space-y-1">
                  <span className="block text-sm font-medium text-gray-700">Description</span>
                  <textarea
                    className="min-h-28 w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                    value={taskForm.description}
                    onChange={(event) =>
                      setTaskForm((prev) => ({ ...prev, description: event.target.value }))
                    }
                    required
                    minLength={10}
                    maxLength={5000}
                  />
                </label>
                <label className="block space-y-1">
                  <span className="block text-sm font-medium text-gray-700">Budget</span>
                  <input
                    className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={taskForm.budgetAmount}
                    onChange={(event) =>
                      setTaskForm((prev) => ({ ...prev, budgetAmount: event.target.value }))
                    }
                    required
                  />
                </label>
                <button
                  className="w-full rounded-md bg-[#fa4616] px-4 py-2 text-sm font-semibold text-white shadow-[0_6px_18px_rgba(250,70,22,0.28)] disabled:opacity-60"
                  type="submit"
                  disabled={createTaskMutation.isPending}
                >
                  {createTaskMutation.isPending ? "Creating..." : "Create task"}
                </button>
                {createTaskError && <p className="text-sm text-red-600">{createTaskError}</p>}
              </form>
            ) : (
              <div className="mt-3 space-y-3">
                <p className="text-sm text-gray-600">Log in to post work to the marketplace.</p>
                <Link
                  className="inline-flex rounded-md bg-[#fa4616] px-4 py-2 text-sm font-semibold text-white shadow-[0_6px_18px_rgba(250,70,22,0.28)]"
                  to="/login"
                >
                  Log in to create
                </Link>
              </div>
            )}
          </section>
        </aside>
      </div>
    </main>
  );
}

function TaskListItem({
  task,
  isSelected,
  onSelect,
}: {
  task: Task;
  isSelected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      className={`rounded-lg border bg-white p-4 text-left transition ${
        isSelected
          ? "border-[#fa4616] ring-2 ring-[#ffd8cc]"
          : "border-[#ece8e3] hover:border-[#d8d1cb]"
      }`}
      type="button"
      onClick={onSelect}
    >
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="font-medium">{task.title}</h3>
          <p className="mt-1 line-clamp-2 text-sm text-gray-600">{task.description}</p>
        </div>
        <span className="rounded-md bg-gray-100 px-2 py-1 text-sm font-medium text-gray-700">
          {formatMoney(task.budgetAmount, task.budgetCurrency)}
        </span>
      </div>
      <p className="mt-3 text-xs text-gray-500">
        Posted by {task.owner.username} on {new Date(task.createdAt).toLocaleDateString()}
      </p>
    </button>
  );
}

function formatMoney(amount: number, currency: string) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
  }).format(amount);
}

function OfferRow({
  offer,
  isOwnerView,
  currentUserId,
  onAccept,
  onWithdraw,
  actionPending,
}: {
  offer: Offer;
  isOwnerView: boolean;
  currentUserId: string;
  onAccept: () => void;
  onWithdraw: () => void;
  actionPending: boolean;
}) {
  const canAccept = isOwnerView && offer.status === "PENDING";
  const canWithdraw = !isOwnerView && offer.seller.id === currentUserId && offer.status === "PENDING";

  return (
    <div className="rounded-md border border-[#e5e0da] bg-white p-3">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm font-medium">{formatMoney(offer.amount, offer.currency)}</p>
        <span className="rounded-full bg-gray-100 px-2 py-1 text-xs font-medium text-gray-700">
          {offer.status}
        </span>
      </div>
      <p className="mt-1 text-sm text-gray-600">{offer.message}</p>
      <p className="mt-1 text-xs text-gray-500">
        By {offer.seller.username} on {new Date(offer.createdAt).toLocaleString()}
      </p>
      {canAccept && (
        <button
          className="mt-2 rounded-md bg-[#fa4616] px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-60"
          type="button"
          onClick={onAccept}
          disabled={actionPending}
        >
          Accept offer
        </button>
      )}
      {canWithdraw && (
        <button
          className="mt-2 rounded-md border border-[#fac7b8] bg-[#fff2ed] px-3 py-1.5 text-xs font-semibold text-[#ba330f] disabled:opacity-60"
          type="button"
          onClick={onWithdraw}
          disabled={actionPending}
        >
          Withdraw offer
        </button>
      )}
    </div>
  );
}

function JobListItem({ task, currentUserId }: { task: Task; currentUserId: string }) {
  const acceptedOffer = task.acceptedOffer;
  const isBuyer = task.owner.id === currentUserId;

  return (
    <div className="rounded-lg border border-[#ece8e3] bg-white p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="font-medium">{task.title}</h3>
          <p className="mt-1 text-sm text-gray-600">{task.description}</p>
        </div>
        <span className="rounded-md bg-[#fff2ed] px-2 py-1 text-xs font-medium text-[#ba330f]">
          ASSIGNED
        </span>
      </div>
      {acceptedOffer && (
        <p className="mt-3 text-sm text-gray-700">
          {isBuyer
            ? `You hired ${acceptedOffer.seller.username} for ${formatMoney(
                acceptedOffer.amount,
                acceptedOffer.currency,
              )}.`
            : `You were hired by ${task.owner.username} for ${formatMoney(
                acceptedOffer.amount,
                acceptedOffer.currency,
              )}.`}
        </p>
      )}
      <p className="mt-2 text-xs text-gray-500">
        Created on {new Date(task.createdAt).toLocaleDateString()}
      </p>
    </div>
  );
}

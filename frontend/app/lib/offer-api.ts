import { request } from "./api-client";

export type OfferStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "WITHDRAWN";

export type OfferSeller = {
  id: string;
  username: string;
};

export type Offer = {
  id: string;
  taskId: string;
  amount: number;
  currency: "USD";
  message: string;
  status: OfferStatus;
  createdAt: string;
  seller: OfferSeller;
};

export type CreateOfferPayload = {
  amount: number;
  currency: "USD";
  message: string;
};

export function listOffers(taskId: string): Promise<Offer[]> {
  return request<Offer[]>(`/api/tasks/${taskId}/offers`, {
    method: "GET",
  });
}

export function createOffer(taskId: string, payload: CreateOfferPayload): Promise<Offer> {
  return request<Offer>(`/api/tasks/${taskId}/offers`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function acceptOffer(taskId: string, offerId: string): Promise<Offer> {
  return request<Offer>(`/api/tasks/${taskId}/offers/${offerId}/accept`, {
    method: "POST",
  });
}

export function withdrawOffer(taskId: string, offerId: string): Promise<Offer> {
  return request<Offer>(`/api/tasks/${taskId}/offers/${offerId}/withdraw`, {
    method: "POST",
  });
}

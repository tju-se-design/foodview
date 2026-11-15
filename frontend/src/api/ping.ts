import { request } from "@/request";

export interface PingResult {
  status: string;
}

export function ping(): Promise<PingResult> {
  return request<PingResult>("/ping");
}

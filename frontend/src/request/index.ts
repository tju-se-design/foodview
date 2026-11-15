/**
 * 统一请求入口，页面和 api/ 只走这里，不直接调 uni.request。
 *
 * H5 走 Vite 代理（/api → localhost:8080）；小程序没有代理，必须给绝对地址，
 * 本地开发用 VITE_API_BASE=http://localhost:8080/api，真机要换成备案过的 HTTPS 域名。
 */

interface Envelope<T> {
  code: number;
  message: string;
  data: T;
}

export class ApiError extends Error {
  readonly code: number;

  constructor(code: number, message: string) {
    super(message);
    this.code = code;
  }
}

// H5 走 Vite 代理，用相对路径即可；小程序没有代理，必须给绝对地址：
// 本地开发用 VITE_API_BASE=http://localhost:8080/api，真机换成备案过的 HTTPS 域名。
let baseUrl = "/api";
// #ifndef H5
baseUrl = import.meta.env.VITE_API_BASE ?? "http://localhost:8080/api";
// #endif

export function request<T>(
  path: string,
  options: Omit<UniApp.RequestOptions, "url" | "success" | "fail"> = {},
): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    uni.request({
      ...options,
      url: `${baseUrl}${path}`,
      success: (response) => {
        const body = response.data as Envelope<T> | undefined;
        if (response.statusCode >= 200 && response.statusCode < 300 && body?.code === 0) {
          resolve(body.data);
          return;
        }
        reject(new ApiError(body?.code ?? response.statusCode, body?.message ?? `HTTP ${response.statusCode}`));
      },
      fail: (error) => reject(new ApiError(-1, error.errMsg)),
    });
  });
}

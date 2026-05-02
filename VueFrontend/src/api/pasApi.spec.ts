import { afterEach, describe, expect, it, vi } from "vitest";

import { ApiError, createGenerationTask, login } from "./pasApi";

describe("pasApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("keeps protected 403 responses as regular API errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        json: async () => ({ message: "Forbidden" }),
      }),
    );

    await expect(createGenerationTask("class Demo {}")).rejects.toBeInstanceOf(
      ApiError,
    );
    await expect(createGenerationTask("class Demo {}")).rejects.toMatchObject({
      status: 403,
      message: "Forbidden",
    });
  });

  it("preserves readable backend messages for business failures", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 422,
        json: async () => ({
          message: "Unsupported algorithm or low confidence",
        }),
      }),
    );

    await expect(login("teacher", "bad-pass")).rejects.toThrow(
      "Unsupported algorithm or low confidence",
    );
  });

  it("uses same-origin api paths by default", async () => {
    const fetchSpy = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        token: "token",
        user: { id: 1, username: "teacher", creditsBalance: 100 },
      }),
    });
    vi.stubGlobal("fetch", fetchSpy);

    await login("teacher", "teacher-pass");

    expect(fetchSpy).toHaveBeenCalledWith(
      "/api/auth/login",
      expect.any(Object),
    );
  });
});

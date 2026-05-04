import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";

import App from "./App.vue";
import * as api from "./api/pasApi";

vi.mock("./api/pasApi", () => ({
  clearStoredToken: vi.fn(),
  createGenerationTask: vi.fn(),
  createRechargeOrder: vi.fn(),
  getGenerationTask: vi.fn(),
  createExportTask: vi.fn(),
  getExportTask: vi.fn(),
  getRecentGenerationTasks: vi.fn(),
  getRecentExportTasks: vi.fn(),
  getRecentRechargeOrders: vi.fn(),
  getRechargePackages: vi.fn(),
  getStoredToken: vi.fn(),
  login: vi.fn(),
  me: vi.fn(),
  register: vi.fn(),
  setStoredToken: vi.fn(),
  simulateRechargePayment: vi.fn(),
}));

const mockUser = {
  id: 1,
  username: "teacher",
  creditsBalance: 300,
  frozenCreditsBalance: 0,
  status: "ACTIVE",
};

const mockCompletedTask = {
  id: 1,
  status: "COMPLETED",
  language: "java",
  detectedAlgorithm: "QUICK_SORT",
  summary: "Quick sort picks a pivot and partitions the array.",
  confidenceScore: 0.93,
  visualizationPayload: {
    algorithm: "QUICK_SORT",
    steps: [
      {
        title: "Compare",
        narration: "Compare values around the pivot.",
        arrayState: [5, 1, 4],
        activeIndices: [0, 1],
        highlightedLines: [3, 4],
      },
    ],
  },
  errorMessage: null,
  creditsCharged: 8,
};

describe("App", () => {
  beforeEach(() => {
    vi.resetAllMocks();
    vi.useRealTimers();
    vi.mocked(api.getStoredToken).mockReturnValue("token");
    vi.mocked(api.me).mockResolvedValue(mockUser);
    vi.mocked(api.getRecentGenerationTasks).mockResolvedValue([]);
    vi.mocked(api.getRecentExportTasks).mockResolvedValue([]);
    vi.mocked(api.getRechargePackages).mockResolvedValue([]);
    vi.mocked(api.getRecentRechargeOrders).mockResolvedValue([]);
  });

  async function mountAuthenticatedApp() {
    const wrapper = mount(App);
    await flushPromises();
    return wrapper;
  }

  it("centers the login panel when unauthenticated", async () => {
    vi.mocked(api.getStoredToken).mockReturnValue(null);

    const wrapper = mount(App);
    await flushPromises();

    expect(wrapper.find(".app-shell").classes()).toContain("app-shell-auth");
  });

  it("restores the logged-in user on mount", async () => {
    const wrapper = await mountAuthenticatedApp();

    expect(api.me).toHaveBeenCalledTimes(1);
    expect(api.getRecentGenerationTasks).toHaveBeenCalledTimes(1);
    expect(api.getRecentExportTasks).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain("teacher");
    expect(wrapper.text()).toContain("可用额度：300");
  });

  it("renders recent history in the sidebar after auth restore", async () => {
    vi.mocked(api.getRecentGenerationTasks).mockResolvedValue([
      {
        id: 42,
        status: "COMPLETED",
        detectedAlgorithm: "QUICK_SORT",
        summary: "Quick sort picks a pivot.",
        sourcePreview: "public class QuickSort {",
        createdAt: "2026-04-22T10:00:00Z",
        updatedAt: "2026-04-22T10:01:00Z",
      },
    ]);
    vi.mocked(api.getRecentExportTasks).mockResolvedValue([
      {
        id: 101,
        generationTaskId: 42,
        status: "COMPLETED",
        detectedAlgorithm: "QUICK_SORT",
        createdAt: "2026-04-22T10:00:00Z",
        updatedAt: "2026-04-22T10:02:00Z",
      },
    ]);

    const wrapper = await mountAuthenticatedApp();

    expect(wrapper.text()).toContain("最近生成");
    expect(wrapper.text()).toContain("最近导出");
    await wrapper.findAll(".sidebar-nav-item")[1].trigger("click");
    expect(
      wrapper.find('[data-generation-history-item="42"]').text(),
    ).toContain("快速排序");
    expect(
      wrapper.find('[data-generation-history-item="42"]').text(),
    ).toContain("2026-04-22 10:01");
    expect(wrapper.find('[data-generation-history-item="42"]').exists()).toBe(
      true,
    );
    await wrapper.findAll(".sidebar-nav-item")[2].trigger("click");
    expect(wrapper.find('[data-export-history-item="101"]').exists()).toBe(
      true,
    );
    expect(wrapper.find('[data-export-history-item="101"]').text()).toContain(
      "2026-04-22 10:02",
    );
  });

  it("shows a workspace overview when no task is selected", async () => {
    const wrapper = await mountAuthenticatedApp();

    expect(wrapper.text()).toContain("智能教学工作台");
    expect(wrapper.text()).not.toContain("选择历史讲解，或开始新的教学演示");
    expect(wrapper.find(".workspace-hero").exists()).toBe(false);
  });

  it("loads a recent generation from the sidebar", async () => {
    vi.mocked(api.getRecentGenerationTasks).mockResolvedValue([
      {
        id: 42,
        status: "COMPLETED",
        detectedAlgorithm: "QUICK_SORT",
        summary: "Quick sort picks a pivot.",
        sourcePreview: "public class QuickSort {",
        createdAt: "2026-04-22T10:00:00Z",
        updatedAt: "2026-04-22T10:01:00Z",
      },
    ]);
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      ...mockCompletedTask,
      sourceCode: "public class QuickSort {}",
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.findAll(".sidebar-nav-item")[1].trigger("click");
    await wrapper.find('[data-generation-history-item="42"]').trigger("click");
    await flushPromises();

    expect(api.getGenerationTask).toHaveBeenCalledWith(42);
    expect(wrapper.text()).toContain("快速排序");
    expect(wrapper.text()).toContain("步骤讲解");
    expect(wrapper.find(".visualization-stage").exists()).toBe(true);
  });

  it("reopens a failed recent generation as a recoverable editor state", async () => {
    vi.mocked(api.getRecentGenerationTasks).mockResolvedValue([
      {
        id: 51,
        status: "FAILED",
        detectedAlgorithm: null,
        summary: null,
        sourcePreview: "class BadAlgorithm {",
        createdAt: "2026-04-22T10:00:00Z",
        updatedAt: "2026-04-22T10:01:00Z",
      },
    ]);
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      id: 51,
      status: "FAILED",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: "Unsupported algorithm or low confidence",
      creditsCharged: 0,
      sourceCode: "class BadAlgorithm {}",
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.findAll(".sidebar-nav-item")[1].trigger("click");
    await wrapper.find('[data-generation-history-item="51"]').trigger("click");
    await flushPromises();

    expect(wrapper.find("textarea").exists()).toBe(true);
    expect(wrapper.text()).toContain("Unsupported algorithm or low confidence");
  });

  it("loads a recent export from the sidebar", async () => {
    vi.mocked(api.getRecentExportTasks).mockResolvedValue([
      {
        id: 101,
        generationTaskId: 42,
        status: "COMPLETED",
        detectedAlgorithm: "QUICK_SORT",
        createdAt: "2026-04-22T10:00:00Z",
        updatedAt: "2026-04-22T10:02:00Z",
      },
    ]);
    vi.mocked(api.getExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 42,
      status: "COMPLETED",
      progress: 100,
      videoUrl: "/api/export-tasks/101/download",
      subtitleUrl: "/files/101.srt",
      audioUrl: "/files/101.mp3",
      errorMessage: null,
      creditsFrozen: 18,
      creditsCharged: 1231,
      createdAt: "2026-04-22T10:00:00Z",
      updatedAt: "2026-04-22T10:02:00Z",
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      ...mockCompletedTask,
      sourceCode: "public class QuickSort {}",
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.findAll(".sidebar-nav-item")[2].trigger("click");
    await wrapper.find('[data-export-history-item="101"]').trigger("click");
    await flushPromises();

    expect(api.getExportTask).toHaveBeenCalledWith(101);
    expect(api.getGenerationTask).toHaveBeenCalledWith(42);
    expect(wrapper.find("[data-download-button]").exists()).toBe(true);
  });

  it("clears a stale token when auth restore fails with auth expiry", async () => {
    vi.mocked(api.me).mockRejectedValue(
      Object.assign(new Error("Login expired. Please sign in again."), {
        name: "AuthExpiredError",
        status: 401,
      }),
    );

    const wrapper = mount(App);
    await flushPromises();

    expect(api.clearStoredToken).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain("登录");
  });

  it("keeps stored token when auth restore fails for a non-auth reason", async () => {
    vi.mocked(api.me).mockRejectedValue(new Error("Network down"));

    const wrapper = mount(App);
    await flushPromises();

    expect(api.clearStoredToken).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("无法恢复登录状态，请重新登录");
  });

  it("does not log out when recharge page hits a non-auth 403 error", async () => {
    vi.mocked(api.getRecentRechargeOrders).mockRejectedValue(
      Object.assign(new Error("FORBIDDEN"), {
        status: 403,
      }),
    );

    const wrapper = await mountAuthenticatedApp();
    await wrapper.findAll(".sidebar-nav-item")[3].trigger("click");
    await flushPromises();

    expect(api.clearStoredToken).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain("购买额度");
    expect(wrapper.text()).toContain("FORBIDDEN");
  });

  it("shows recharge order time in the recharge history", async () => {
    vi.mocked(api.getRecentRechargeOrders).mockResolvedValue([
      {
        id: 7,
        packageCode: "studio",
        packageName: "工作室包",
        credits: 2000,
        amountCents: 19900,
        status: "PAID",
        createdAt: "2026-04-22T10:00:00Z",
        paidAt: "2026-04-22T10:03:04Z",
      },
    ]);

    const wrapper = await mountAuthenticatedApp();
    await wrapper.findAll(".sidebar-nav-item")[3].trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("工作室包");
    expect(wrapper.text()).toContain("2026-04-22 10:03");
    expect(wrapper.text()).toContain("2000 额度");
    expect(wrapper.text()).toContain("已入账");
  });

  it("returns to login when a protected action rejects with auth expiry", async () => {
    vi.mocked(api.createGenerationTask).mockRejectedValue(
      Object.assign(new Error("Login expired. Please sign in again."), {
        name: "AuthExpiredError",
        status: 401,
      }),
    );

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("textarea").setValue("class Demo {}");
    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(api.clearStoredToken).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain("登录已过期，请重新登录");
    expect(wrapper.text()).toContain("登录");
  });

  it("logs in and stores the JWT token", async () => {
    vi.mocked(api.getStoredToken).mockReturnValue(null);
    vi.mocked(api.me).mockRejectedValue(new Error("Auth check failed"));
    vi.mocked(api.login).mockResolvedValue({
      token: "jwt-token",
      user: mockUser,
    });

    const wrapper = mount(App);
    await flushPromises();

    await wrapper.find('input[placeholder="用户名"]').setValue("teacher");
    await wrapper.find('input[placeholder="密码"]').setValue("secret-pass");
    await wrapper.find(".auth-form").trigger("submit");
    await flushPromises();

    expect(api.login).toHaveBeenCalledWith("teacher", "secret-pass");
    expect(api.setStoredToken).toHaveBeenCalledWith("jwt-token");
    expect(wrapper.text()).toContain("teacher");
  });

  it("submits registration from the auth panel and returns to login mode", async () => {
    vi.mocked(api.getStoredToken).mockReturnValue(null);
    vi.mocked(api.me).mockRejectedValue(new Error("Auth check failed"));
    vi.mocked(api.register).mockResolvedValue(mockUser);

    const wrapper = mount(App);
    await flushPromises();

    await wrapper.find(".auth-toggle").trigger("click");
    await wrapper.find('input[placeholder="用户名"]').setValue("new-user");
    await wrapper.find('input[placeholder="密码"]').setValue("secret-pass");
    await wrapper.find('input[placeholder="确认密码"]').setValue("secret-pass");
    await wrapper.find(".auth-form").trigger("submit");
    await flushPromises();

    expect(api.register).toHaveBeenCalledWith("new-user", "secret-pass");
    expect(wrapper.text()).toContain("注册成功，请登录");
    expect(wrapper.text()).toContain("欢迎回来");
  });

  it("logs out and clears the stored token", async () => {
    const wrapper = await mountAuthenticatedApp();

    await wrapper.find(".sidebar-footer button").trigger("click");

    expect(api.clearStoredToken).toHaveBeenCalledTimes(1);
    expect(wrapper.text()).toContain("登录");
  });

  it("starts over without wiping the editor source", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask);

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("textarea").setValue("public class MergeSort {}");
    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await wrapper.find("[data-start-over-button]").trigger("click");

    expect(
      (wrapper.find("textarea").element as HTMLTextAreaElement).value,
    ).toContain("MergeSort");
    expect(wrapper.find("[data-export-button]").exists()).toBe(false);
  });

  it("switches from editor to playback after a successful generation", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask);

    const wrapper = await mountAuthenticatedApp();

    await wrapper.find("textarea").setValue("public class QuickSort {}");
    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(wrapper.find(".task-summary-card h2").text()).toContain("快速排序");
    expect(wrapper.text()).toContain("Compare");
  });

  it("shows a continue generation action while a task is queued", async () => {
    const pendingTask = {
      id: 8,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
      sourceCode: "public class QuickSort {}",
    };
    vi.mocked(api.createGenerationTask).mockResolvedValue(pendingTask);
    vi.mocked(api.getGenerationTask).mockResolvedValue(pendingTask);

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("textarea").setValue("public class QuickSort {}");
    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(wrapper.text()).toContain("排队中");
    expect(wrapper.find("[data-continue-generation-button]").text()).toContain("继续生成");
    expect(wrapper.text()).not.toContain("重新开始");
  });

  it("updates the starter code when the selected language changes", async () => {
    const wrapper = await mountAuthenticatedApp();

    await wrapper.find("select").setValue("cpp");

    expect((wrapper.find("textarea").element as HTMLTextAreaElement).value).toContain(
      "void quickSort(vector<int>& array, int low, int high)",
    );
    expect((wrapper.find("textarea").element as HTMLTextAreaElement).value).not.toContain(
      "public class QuickSort",
    );
  });

  it("keeps custom code when the selected language changes", async () => {
    const wrapper = await mountAuthenticatedApp();

    await wrapper.find("textarea").setValue("custom algorithm code");
    await wrapper.find("select").setValue("c");

    expect((wrapper.find("textarea").element as HTMLTextAreaElement).value).toBe(
      "custom algorithm code",
    );
  });

  it("shows a readable error when generation is rejected", async () => {
    vi.mocked(api.createGenerationTask).mockRejectedValue(
      new Error("Unsupported algorithm or low confidence"),
    );

    const wrapper = await mountAuthenticatedApp();

    await wrapper.find("textarea").setValue("class Knapsack {}");
    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(wrapper.text()).toContain("Unsupported algorithm or low confidence");
  });

  it("shows generation failure message when polling returns failed", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      id: 1,
      status: "FAILED",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: "Unsupported algorithm or low confidence",
      creditsCharged: 0,
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(wrapper.text()).toContain("Unsupported algorithm or low confidence");
  });

  it("shows generation status while queued or processing", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      id: 1,
      status: "ANALYZING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(wrapper.text()).toContain("生成状态");
    expect(wrapper.text()).toContain("分析中");
    expect(wrapper.find(".visualization-stage").exists()).toBe(false);
  });

  it("allows starting over while generation is in progress", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      id: 1,
      status: "ANALYZING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await wrapper.find("[data-start-over-button]").trigger("click");

    expect(wrapper.find("textarea").exists()).toBe(true);
    expect(wrapper.find(".generation-status-card").exists()).toBe(false);
  });

  it("returns to a recoverable editor state when initial generation refresh fails", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockRejectedValue(
      new Error("Generation polling failed"),
    );

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("textarea").setValue("class Demo {}");
    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(wrapper.find("textarea").exists()).toBe(true);
    expect(wrapper.text()).toContain("Generation polling failed");
  });

  it("creates and renders export progress after clicking export", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask);
    vi.mocked(api.createExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: "PENDING",
      progress: 0,
      creditsFrozen: 18,
    });
    vi.mocked(api.getExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: "COMPLETED",
      progress: 100,
      videoUrl: "/api/export-tasks/101/download",
      subtitleUrl: "/files/101.srt",
      audioUrl: "/files/101.mp3",
      errorMessage: null,
      creditsFrozen: 18,
      creditsCharged: 1231,
      createdAt: "2026-04-11T16:00:00Z",
      updatedAt: "2026-04-11T16:00:30Z",
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await wrapper.find("[data-export-button]").trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("导出状态");
    expect(wrapper.text()).toContain("已完成");
    expect(wrapper.find("[data-download-button]").exists()).toBe(true);
  });

  it("disables export while an export task is active", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask);
    vi.mocked(api.createExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: "PENDING",
      progress: 0,
      creditsFrozen: 18,
    });
    vi.mocked(api.getExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: "PROCESSING",
      progress: 45,
      videoUrl: null,
      subtitleUrl: null,
      audioUrl: null,
      errorMessage: null,
      creditsFrozen: 18,
      creditsCharged: null,
      createdAt: "2026-04-11T16:00:00Z",
      updatedAt: "2026-04-11T16:00:10Z",
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await wrapper.find("[data-export-button]").trigger("click");
    await flushPromises();

    expect(
      wrapper.find("[data-export-button]").attributes("disabled"),
    ).toBeDefined();
    expect(wrapper.text()).toContain(
      "视频正在导出，你可以留在当前页面等待完成",
    );
  });

  it("shows export failure message when polling returns failed", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask);
    vi.mocked(api.createExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: "PENDING",
      progress: 0,
      creditsFrozen: 18,
    });
    vi.mocked(api.getExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: "FAILED",
      progress: 100,
      videoUrl: null,
      subtitleUrl: null,
      audioUrl: null,
      errorMessage: "VIDEO_COMPOSE_FAILED",
      creditsFrozen: 18,
      creditsCharged: null,
      createdAt: "2026-04-11T16:00:00Z",
      updatedAt: "2026-04-11T16:00:10Z",
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await wrapper.find("[data-export-button]").trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("VIDEO_COMPOSE_FAILED");
  });

  it("shows retry export action after export failure", async () => {
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask);
    vi.mocked(api.createExportTask)
      .mockResolvedValueOnce({
        id: 101,
        generationTaskId: 1,
        status: "PENDING",
        progress: 0,
        creditsFrozen: 18,
      })
      .mockResolvedValueOnce({
        id: 102,
        generationTaskId: 1,
        status: "PENDING",
        progress: 0,
        creditsFrozen: 18,
      });
    vi.mocked(api.getExportTask).mockResolvedValue({
      id: 101,
      generationTaskId: 1,
      status: "FAILED",
      progress: 100,
      videoUrl: null,
      subtitleUrl: null,
      audioUrl: null,
      errorMessage: "Video composition failed.",
      creditsFrozen: 18,
      creditsCharged: null,
      createdAt: "2026-04-11T16:00:00Z",
      updatedAt: "2026-04-11T16:00:10Z",
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("form").trigger("submit");
    await flushPromises();
    await wrapper.find("[data-export-button]").trigger("click");
    await flushPromises();

    expect(wrapper.find("[data-retry-export-button]").exists()).toBe(true);
  });

  it("auto advances steps while playing and stops at the end", async () => {
    vi.useFakeTimers();
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      ...mockCompletedTask,
      visualizationPayload: {
        algorithm: "QUICK_SORT",
        steps: [
          {
            title: "Step 1",
            narration: "First",
            arrayState: [5, 1, 4],
            activeIndices: [0, 1],
            highlightedLines: [3, 4],
          },
          {
            title: "Step 2",
            narration: "Second",
            arrayState: [1, 5, 4],
            activeIndices: [1, 2],
            highlightedLines: [5],
          },
        ],
      },
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("form").trigger("submit");
    await flushPromises();

    expect(wrapper.text()).toContain("步骤 1 / 2");
    await wrapper.find("[data-play-toggle]").trigger("click");
    await vi.advanceTimersByTimeAsync(1800);

    expect(wrapper.text()).toContain("步骤 2 / 2");
    expect(wrapper.find("[data-play-toggle]").text()).toContain("播放");
  });

  it("allows scrubbing timeline and changing playback speed", async () => {
    vi.useFakeTimers();
    vi.mocked(api.createGenerationTask).mockResolvedValue({
      id: 1,
      status: "PENDING",
      language: "java",
      detectedAlgorithm: null,
      summary: null,
      confidenceScore: 0,
      visualizationPayload: null,
      errorMessage: null,
      creditsCharged: 0,
    });
    vi.mocked(api.getGenerationTask).mockResolvedValue({
      ...mockCompletedTask,
      visualizationPayload: {
        algorithm: "QUICK_SORT",
        steps: [
          {
            title: "Step 1",
            narration: "First",
            arrayState: [5, 1, 4],
            activeIndices: [0, 1],
            highlightedLines: [3, 4],
          },
          {
            title: "Step 2",
            narration: "Second",
            arrayState: [1, 5, 4],
            activeIndices: [1, 2],
            highlightedLines: [5],
          },
          {
            title: "Step 3",
            narration: "Third",
            arrayState: [1, 4, 5],
            activeIndices: [2],
            highlightedLines: [6],
          },
        ],
      },
    });

    const wrapper = await mountAuthenticatedApp();
    await wrapper.find("form").trigger("submit");
    await flushPromises();

    await wrapper.find("[data-speed-select]").setValue("2");
    await wrapper.find("[data-step-slider]").setValue("1");
    expect(wrapper.text()).toContain("Step 2");

    await wrapper.find("[data-play-toggle]").trigger("click");
    await vi.advanceTimersByTimeAsync(800);

    expect(wrapper.text()).toContain("步骤 3 / 3");
  });
});

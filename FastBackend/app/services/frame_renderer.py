from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


class FrameRenderer:
    LABELS = {
        "data_structure_view": "数据结构视图",
        "narration": "讲解字幕",
        "code_focus": "源码定位",
        "current_implementation": "当前实现",
    }
    ALGORITHM_NAMES = {
        "BUBBLE_SORT": "冒泡排序",
        "SELECTION_SORT": "选择排序",
        "INSERTION_SORT": "插入排序",
        "BINARY_SEARCH": "二分查找",
        "QUICK_SORT": "快速排序",
        "MERGE_SORT": "归并排序",
        "HEAP_SORT": "堆排序",
        "BFS": "广度优先搜索",
        "DFS": "深度优先搜索",
    }
    STEP_TITLE_PREFIXES = {
        "Compare ": "比较元素",
        "Swap ": "交换元素",
        "Heapify node": "维护堆结构",
        "Visit node": "访问节点",
        "Explore node": "探索节点",
    }
    STEP_TITLES = {
        "Check middle": "检查中点",
        "Choose pivot": "选择基准",
        "Compare to pivot": "与基准比较",
        "Move left of pivot": "移到基准左侧",
        "Place pivot": "放置基准",
        "Quick sort complete": "快速排序完成",
        "Merge sort complete": "归并排序完成",
        "Bubble sort complete": "冒泡排序完成",
        "Selection sort complete": "选择排序完成",
        "Insertion sort complete": "插入排序完成",
        "Heap sort complete": "堆排序完成",
        "Scan for minimum": "扫描最小值",
        "Update minimum": "更新最小值",
        "Swap into place": "交换到位",
        "Pick key": "取出待插入值",
        "Shift right": "右移元素",
        "Insert key": "插入到位",
        "Move max to sorted suffix": "最大值移入有序区",
        "Restore heap order": "恢复堆顺序",
        "Split range": "拆分区间",
        "Merge next value": "合并下一个值",
        "Append remaining left": "追加左侧剩余值",
        "Append remaining right": "追加右侧剩余值",
    }

    def __init__(self, output_root: Path) -> None:
        self.output_root = output_root
        self.scale = 1.5
        self.width = self._s(1280)
        self.height = self._s(720)
        self.font_regular = self._load_font(["NotoSansCJK-Regular.ttc", "NotoSansSC-Regular.otf", "msyh.ttc", "Microsoft YaHei.ttf", "DejaVuSans.ttf"], 20)
        self.font_small = self._load_font(["NotoSansCJK-Regular.ttc", "NotoSansSC-Regular.otf", "msyh.ttc", "Microsoft YaHei.ttf", "DejaVuSans.ttf"], 16)
        self.font_title = self._load_font(["NotoSansCJK-Bold.ttc", "NotoSansSC-Bold.otf", "msyhbd.ttc", "Microsoft YaHei Bold.ttf", "DejaVuSans-Bold.ttf"], 24)
        self.font_code = self._load_font(["CascadiaMono.ttf", "Consola.ttf", "consola.ttf", "DejaVuSansMono.ttf", "NotoSansMono-Regular.ttf"], 16)

    def render_frames(self, payload: dict) -> Path:
        export_task_id = payload["exportTaskId"]
        source_code = payload.get("sourceCode", "")
        frame_dir = self.output_root / str(export_task_id) / "frames"
        frame_dir.mkdir(parents=True, exist_ok=True)

        for index, step in enumerate(payload["visualizationPayload"]["steps"], start=1):
            image = self._create_background()
            draw = ImageDraw.Draw(image)

            self._draw_header(
                draw,
                payload["visualizationPayload"].get("algorithm", "ALGORITHM"),
                index,
                len(payload["visualizationPayload"]["steps"]),
            )
            self._draw_stage(draw, step)
            self._draw_explanation(draw, step)
            self._draw_code_panel(draw, source_code, step.get("highlightedLines", []))

            image.save(frame_dir / f"frame-{index:04d}.png")

        return frame_dir

    def _create_background(self) -> Image.Image:
        image = Image.new("RGB", (self.width, self.height), color=(15, 23, 42))
        draw = ImageDraw.Draw(image)
        for y in range(self.height):
            blend = y / (self.height - 1)
            color = (
                int(15 + 8 * blend),
                int(23 + 14 * blend),
                int(42 + 28 * blend),
            )
            draw.line((0, y, self.width, y), fill=color)
        draw.rounded_rectangle(
            self._box(32, 24, 1248, 696), radius=self._s(30), outline=(51, 65, 85), width=self._s(2)
        )
        return image

    def _draw_header(
        self,
        draw: ImageDraw.ImageDraw,
        algorithm: str,
        step_index: int,
        step_count: int,
    ) -> None:
        draw.rounded_rectangle(self._box(56, 40, 324, 92), radius=self._s(18), fill=(30, 41, 59))
        draw.text(self._point(80, 56), self._display_algorithm(algorithm), fill=(224, 242, 254), font=self.font_small)
        draw.rounded_rectangle(self._box(1070, 40, 1216, 92), radius=self._s(18), fill=(14, 165, 233))
        draw.text(self._point(1100, 56), f"{step_index}/{step_count}", fill=(255, 255, 255), font=self.font_small)

    def _draw_stage(self, draw: ImageDraw.ImageDraw, step: dict) -> None:
        draw.rounded_rectangle(self._box(56, 120, 730, 520), radius=self._s(28), fill=(20, 30, 50))
        draw.text(self._point(84, 148), self._display_step_title(step), fill=(255, 255, 255), font=self.font_title)
        draw.text(self._point(84, 184), self._label("data_structure_view"), fill=(148, 163, 184), font=self.font_small)

        values = step["arrayState"]
        bar_positions = self._bar_positions(values)
        active_indices = set(step.get("activeIndices", []))

        for bar_index, (left, bar_width) in enumerate(bar_positions):
            value = values[bar_index]
            top = 450 - value * 22
            right = left + bar_width
            active = bar_index in active_indices
            fill = (249, 115, 22) if active else (56, 189, 248)
            draw.rounded_rectangle(self._box(left, top, right, 450), radius=self._s(18), fill=fill)
            draw.text(self._point(left + 18, top - 28), str(value), fill=(255, 255, 255), font=self.font_small)

    def _draw_explanation(self, draw: ImageDraw.ImageDraw, step: dict) -> None:
        draw.rounded_rectangle(self._box(56, 548, 730, 664), radius=self._s(24), fill=(38, 50, 76))
        draw.text(self._point(84, 574), self._label("narration"), fill=(125, 211, 252), font=self.font_small)
        self._draw_wrapped_text(draw, step["narration"], 84, 608, 590, self.font_regular, (226, 232, 240), 28)

    def _draw_code_panel(
        self, draw: ImageDraw.ImageDraw, source_code: str, highlighted_lines: list[int]
    ) -> None:
        draw.rounded_rectangle(self._box(760, 120, 1216, 664), radius=self._s(28), fill=(26, 34, 58))
        draw.text(self._point(788, 146), self._label("code_focus"), fill=(255, 255, 255), font=self.font_title)
        draw.text(self._point(788, 178), self._label("current_implementation"), fill=(148, 163, 184), font=self.font_small)

        lines = self._visible_code_lines(source_code, highlighted_lines)
        line_y = 226
        for index, line in lines:
            if index in highlighted_lines:
                draw.rounded_rectangle(
                    self._box(782, line_y - 4, 1194, line_y + 24), radius=self._s(10), fill=(14, 165, 233)
                )
                fill = (255, 255, 255)
            else:
                fill = (203, 213, 225)
            draw.text(self._point(792, line_y), f"{index:>2}", fill=fill, font=self.font_code)
            fitted_line = self._fit_text(draw, line, 320, self.font_code)
            draw.text(self._point(844, line_y), fitted_line, fill=fill, font=self.font_code)
            line_y += 28

    def _bar_positions(self, values: list[int]) -> list[tuple[int, int]]:
        spacing = 18
        left_bound = 92
        right_bound = 704
        count = max(1, len(values))
        available = right_bound - left_bound - spacing * (count - 1)
        bar_width = max(28, min(72, int(available / count)))
        total_width = bar_width * count + spacing * (count - 1)
        left_start = left_bound + max(0, int((right_bound - left_bound - total_width) / 2))
        return [(left_start + index * (bar_width + spacing), bar_width) for index in range(len(values))]

    def _visible_code_lines(self, source_code: str, highlighted_lines: list[int], max_lines: int = 14) -> list[tuple[int, str]]:
        lines = source_code.splitlines() or [""]
        valid_highlights = sorted(line for line in highlighted_lines if 1 <= line <= len(lines))
        first_line = valid_highlights[0] if valid_highlights else 1
        start_line = max(1, first_line - max_lines // 2)
        start_line = min(start_line, max(1, len(lines) - max_lines + 1))
        end_line = min(len(lines), start_line + max_lines - 1)
        return [(line_number, lines[line_number - 1]) for line_number in range(start_line, end_line + 1)]

    def _fit_text(self, draw: ImageDraw.ImageDraw, text: str, max_width: int, font: ImageFont.ImageFont) -> str:
        if draw.textlength(text, font=font) <= self._s(max_width):
            return text
        trimmed = text
        while trimmed and draw.textlength(trimmed, font=font) > self._s(max_width):
            trimmed = trimmed[:-1]
        return trimmed.rstrip()

    def _display_algorithm(self, algorithm: str) -> str:
        return self.ALGORITHM_NAMES.get(algorithm, algorithm.replace("_", " "))

    def _display_step_title(self, step_or_title) -> str:
        if isinstance(step_or_title, dict):
            display_title = step_or_title.get("displayTitle")
            if display_title:
                return display_title
            title = step_or_title.get("title", "")
        else:
            title = step_or_title
        if title in self.STEP_TITLES:
            return self.STEP_TITLES[title]
        for prefix, translated in self.STEP_TITLE_PREFIXES.items():
            if title.startswith(prefix):
                return translated
        return title

    def _label(self, key: str) -> str:
        return self.LABELS[key]

    def _s(self, value: int | float) -> int:
        return int(round(value * self.scale))

    def _point(self, x: int | float, y: int | float) -> tuple[int, int]:
        return self._s(x), self._s(y)

    def _box(self, left: int | float, top: int | float, right: int | float, bottom: int | float) -> tuple[int, int, int, int]:
        return self._s(left), self._s(top), self._s(right), self._s(bottom)

    def _load_font(self, candidates: list[str], size: int) -> ImageFont.ImageFont:
        scaled_size = self._s(size)
        for candidate in candidates:
            try:
                return ImageFont.truetype(candidate, scaled_size)
            except OSError:
                continue
        return ImageFont.load_default(scaled_size)

    def _draw_wrapped_text(
        self,
        draw: ImageDraw.ImageDraw,
        text: str,
        x: int,
        y: int,
        max_width: int,
        font: ImageFont.ImageFont,
        fill: tuple[int, int, int],
        line_height: int,
    ) -> None:
        current_line = ""
        current_y = y
        for char in text:
            candidate = current_line + char
            if draw.textlength(candidate, font=font) <= self._s(max_width):
                current_line = candidate
                continue
            draw.text(self._point(x, current_y), current_line, fill=fill, font=font)
            current_line = char
            current_y += line_height
        if current_line:
            draw.text(self._point(x, current_y), current_line, fill=fill, font=font)

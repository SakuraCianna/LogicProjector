from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


class FrameRenderer:
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
        draw.text(self._point(80, 56), f"{algorithm.replace('_', ' ')}", fill=(224, 242, 254), font=self.font_small)
        draw.rounded_rectangle(self._box(1070, 40, 1216, 92), radius=self._s(18), fill=(14, 165, 233))
        draw.text(self._point(1100, 56), f"{step_index}/{step_count}", fill=(255, 255, 255), font=self.font_small)

    def _draw_stage(self, draw: ImageDraw.ImageDraw, step: dict) -> None:
        draw.rounded_rectangle(self._box(56, 120, 820, 520), radius=self._s(28), fill=(20, 30, 50))
        draw.text(self._point(84, 148), step["title"], fill=(255, 255, 255), font=self.font_title)
        draw.text(self._point(84, 184), "Data structure view", fill=(148, 163, 184), font=self.font_small)

        values = step["arrayState"]
        bar_width = max(56, int(620 / max(1, len(values))))
        spacing = 18
        left_start = 92
        active_indices = set(step.get("activeIndices", []))

        for bar_index, value in enumerate(values):
            left = left_start + bar_index * (bar_width + spacing)
            top = 450 - value * 22
            right = left + bar_width
            active = bar_index in active_indices
            fill = (249, 115, 22) if active else (56, 189, 248)
            draw.rounded_rectangle(self._box(left, top, right, 450), radius=self._s(18), fill=fill)
            draw.text(self._point(left + 18, top - 28), str(value), fill=(255, 255, 255), font=self.font_small)

    def _draw_explanation(self, draw: ImageDraw.ImageDraw, step: dict) -> None:
        draw.rounded_rectangle(self._box(56, 548, 820, 664), radius=self._s(24), fill=(38, 50, 76))
        draw.text(self._point(84, 574), "Narration", fill=(125, 211, 252), font=self.font_small)
        self._draw_wrapped_text(draw, step["narration"], 84, 608, 700, self.font_regular, (226, 232, 240), 28)

    def _draw_code_panel(
        self, draw: ImageDraw.ImageDraw, source_code: str, highlighted_lines: list[int]
    ) -> None:
        draw.rounded_rectangle(self._box(860, 120, 1216, 664), radius=self._s(28), fill=(26, 34, 58))
        draw.text(self._point(888, 146), "Code focus", fill=(255, 255, 255), font=self.font_title)
        draw.text(self._point(888, 178), "Current implementation", fill=(148, 163, 184), font=self.font_small)

        lines = source_code.splitlines()[:12]
        line_y = 226
        for index, line in enumerate(lines, start=1):
            if index in highlighted_lines:
                draw.rounded_rectangle(
                    self._box(882, line_y - 4, 1194, line_y + 24), radius=self._s(10), fill=(14, 165, 233)
                )
                fill = (255, 255, 255)
            else:
                fill = (203, 213, 225)
            draw.text(self._point(892, line_y), f"{index:>2} {line[:34]}", fill=fill, font=self.font_code)
            line_y += 32

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

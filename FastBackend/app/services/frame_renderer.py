from pathlib import Path

from PIL import Image, ImageDraw


class FrameRenderer:
    def __init__(self, output_root: Path) -> None:
        self.output_root = output_root

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
        image = Image.new("RGB", (1280, 720), color=(15, 23, 42))
        draw = ImageDraw.Draw(image)
        for y in range(720):
            blend = y / 719
            color = (
                int(15 + 8 * blend),
                int(23 + 14 * blend),
                int(42 + 28 * blend),
            )
            draw.line((0, y, 1280, y), fill=color)
        draw.rounded_rectangle(
            (32, 24, 1248, 696), radius=30, outline=(51, 65, 85), width=2
        )
        return image

    def _draw_header(
        self,
        draw: ImageDraw.ImageDraw,
        algorithm: str,
        step_index: int,
        step_count: int,
    ) -> None:
        draw.rounded_rectangle((56, 40, 324, 92), radius=18, fill=(30, 41, 59))
        draw.text((80, 56), f"{algorithm.replace('_', ' ')}", fill=(224, 242, 254))
        draw.rounded_rectangle((1070, 40, 1216, 92), radius=18, fill=(14, 165, 233))
        draw.text((1100, 56), f"{step_index}/{step_count}", fill=(255, 255, 255))

    def _draw_stage(self, draw: ImageDraw.ImageDraw, step: dict) -> None:
        draw.rounded_rectangle((56, 120, 820, 520), radius=28, fill=(20, 30, 50))
        draw.text((84, 148), step["title"], fill=(255, 255, 255))
        draw.text((84, 184), "Data structure view", fill=(148, 163, 184))

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
            draw.rounded_rectangle((left, top, right, 450), radius=18, fill=fill)
            draw.text((left + 18, top - 28), str(value), fill=(255, 255, 255))

    def _draw_explanation(self, draw: ImageDraw.ImageDraw, step: dict) -> None:
        draw.rounded_rectangle((56, 548, 820, 664), radius=24, fill=(38, 50, 76))
        draw.text((84, 574), "Narration", fill=(125, 211, 252))
        draw.text((84, 608), step["narration"], fill=(226, 232, 240))

    def _draw_code_panel(
        self, draw: ImageDraw.ImageDraw, source_code: str, highlighted_lines: list[int]
    ) -> None:
        draw.rounded_rectangle((860, 120, 1216, 664), radius=28, fill=(26, 34, 58))
        draw.text((888, 146), "Code focus", fill=(255, 255, 255))
        draw.text((888, 178), "Current implementation", fill=(148, 163, 184))

        lines = source_code.splitlines()[:12]
        line_y = 226
        for index, line in enumerate(lines, start=1):
            if index in highlighted_lines:
                draw.rounded_rectangle(
                    (882, line_y - 4, 1194, line_y + 24), radius=10, fill=(14, 165, 233)
                )
                fill = (255, 255, 255)
            else:
                fill = (203, 213, 225)
            draw.text((892, line_y), f"{index:>2} {line[:34]}", fill=fill)
            line_y += 32

from pathlib import Path

from PIL import Image, ImageDraw


class FrameRenderer:
    def __init__(self, output_root: Path) -> None:
        self.output_root = output_root

    def render_frames(self, payload: dict) -> Path:
        export_task_id = payload["exportTaskId"]
        frame_dir = self.output_root / str(export_task_id) / "frames"
        frame_dir.mkdir(parents=True, exist_ok=True)

        for index, step in enumerate(payload["visualizationPayload"]["steps"], start=1):
            image = Image.new("RGB", (1280, 720), color=(15, 23, 42))
            draw = ImageDraw.Draw(image)
            draw.text((48, 32), step["title"], fill=(255, 255, 255))
            draw.text((48, 88), step["narration"], fill=(186, 230, 253))

            for bar_index, value in enumerate(step["arrayState"]):
                left = 80 + bar_index * 120
                top = 620 - value * 24
                right = left + 72
                active = bar_index in step.get("activeIndices", [])
                fill = (249, 115, 22) if active else (14, 165, 233)
                draw.rectangle((left, top, right, 620), fill=fill)
                draw.text((left + 20, top - 24), str(value), fill=(255, 255, 255))

            image.save(frame_dir / f"frame-{index:04d}.png")

        return frame_dir

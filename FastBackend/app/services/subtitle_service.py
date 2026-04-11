class SubtitleService:
    def build_subtitle_path(self, task_id: int) -> str:
        return f"outputs/{task_id}.srt"

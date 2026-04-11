class TtsService:
    def build_audio_path(self, task_id: int) -> str:
        return f"outputs/{task_id}.mp3"

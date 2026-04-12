from fastapi import FastAPI

from app.models import ExportRequest, ExportResponse
from app.services.export_pipeline import ExportPipeline

app = FastAPI(title="Pas Media Worker")
pipeline = ExportPipeline()


@app.post("/exports", response_model=ExportResponse)
def create_export(request: ExportRequest) -> ExportResponse:
    return ExportResponse(**pipeline.build_export(request.model_dump()))

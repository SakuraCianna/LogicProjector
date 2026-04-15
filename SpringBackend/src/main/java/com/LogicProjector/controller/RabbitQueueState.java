package com.LogicProjector.controller;

public record RabbitQueueState(String name, int messages, int consumers) {
}

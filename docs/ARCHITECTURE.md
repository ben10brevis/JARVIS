# JARVIS architecture notes

## Request lifecycle

1. Input arrives from a UI button, typed text, notification action, or future approved wake-word integration.
2. Normalize and classify the request.
3. Route deterministic phone commands to a registered local skill.
4. Route general questions to an `AIProvider` backend.
5. Validate requested actions against an allow-listed skill registry.
6. Ask for confirmation for sensitive actions.
7. Execute through documented Android APIs / intents.
8. Return a response to the UI and TTS layer.

## Safety and privacy rules

- Never execute arbitrary code or shell commands produced by a model.
- Do not embed AI provider credentials in the Android package.
- Ask before sending messages, placing calls, or taking other consequential actions.
- Request runtime permissions only at the point a feature needs them.
- Make microphone listening visible and user-controlled.
- Provide a clear stop-listening action before implementing background listening.
- Store only the conversation data the user chooses to retain.

## Wake-word integration boundary

`WakeWordEngine` is an abstraction, not an implementation. Prefer Android's system assistant/hotword facilities where available. Any local keyword model must be evaluated for accuracy, false activations, power use, and privacy, and must comply with the OS foreground/background microphone model. Device-specific support must be measured on the OnePlus 15R firmware.

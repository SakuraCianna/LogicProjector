export function withoutSentencePeriod(value: string | null | undefined) {
  return (value ?? '').trimEnd().replace(/[。．.…]+$/u, '')
}

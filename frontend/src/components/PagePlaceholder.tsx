type PagePlaceholderProps = {
  title: string
  useCases: string
  description: string
}

/**
 * Stand-in for a route until its real implementation slice lands. Kept as one
 * component so every placeholder page is a single line, not a copy-pasted block.
 */
export function PagePlaceholder({ title, useCases, description }: PagePlaceholderProps) {
  return (
    <main className="mx-auto flex min-h-[60vh] max-w-2xl flex-col items-start justify-center gap-2 px-6">
      <span className="text-sm font-mono text-neutral-500">{useCases}</span>
      <h1 className="text-2xl font-semibold">{title}</h1>
      <p className="text-neutral-600">{description}</p>
    </main>
  )
}

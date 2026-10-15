type Props = {
  title: string
  posterUrl: string | null
  className?: string
}

/** Portrait poster; movies saved without a poster get a branded title card instead of a broken image. */
export function MoviePoster({ title, posterUrl, className = '' }: Props) {
  if (posterUrl) {
    return <img src={posterUrl} alt={`${title} poster`} loading="lazy" className={`aspect-[2/3] rounded-xl object-cover ${className}`} />
  }
  return (
    <div
      role="img"
      aria-label={`${title} poster`}
      className={`grid aspect-[2/3] place-items-center rounded-xl bg-gradient-to-br from-brand to-neutral-900 p-4 text-center text-lg font-bold text-white ${className}`}
    >
      {title}
    </div>
  )
}

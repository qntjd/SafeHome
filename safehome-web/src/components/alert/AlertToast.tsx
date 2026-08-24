import type { AlertEvent } from '@/types'

interface Props { alert: AlertEvent }

const LEVEL_STYLE = {
  INFO:    { dot: 'beacon-dot--safe',   accent: 'var(--grade-a)' },
  WARNING: { dot: 'beacon-dot--warn',   accent: 'var(--accent-amber)' },
  DANGER:  { dot: 'beacon-dot--danger', accent: 'var(--accent-red)' },
}

export default function AlertToast({ alert }: Props) {
  const ls = LEVEL_STYLE[alert.level]
  return (
    <div
      className="rounded-2xl px-4 py-3 max-w-xs"
      style={{
        background: 'var(--ink)',
        border: '1px solid var(--ink-border)',
        borderLeft: `3px solid ${ls.accent}`,
        boxShadow: 'var(--shadow-beacon)',
      }}
    >
      <p className="text-xs font-semibold mb-1 flex items-center gap-1.5" style={{ color: 'var(--ink-text-muted)' }}>
        <span className={`beacon-dot ${ls.dot}`} />
        {alert.district} · {alert.type}
      </p>
      <p className="text-sm leading-snug" style={{ color: 'var(--ink-text)' }}>{alert.message}</p>
    </div>
  )
}

import type { AlertEvent } from '@/types'

interface Props { alert: AlertEvent }

const LEVEL_STYLE = {
  INFO:    'bg-blue-600',
  WARNING: 'bg-yellow-500',
  DANGER:  'bg-red-600',
}

export default function AlertToast({ alert }: Props) {
  return (
    <div className={`${LEVEL_STYLE[alert.level]} text-white rounded-xl px-4 py-3 shadow-lg max-w-xs`}>
      <p className="text-xs font-semibold opacity-80 mb-0.5">{alert.district} · {alert.type}</p>
      <p className="text-sm leading-snug">{alert.message}</p>
    </div>
  )
}
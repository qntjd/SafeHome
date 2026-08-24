import type { TripResponse } from '@/api/trip'

interface Props {
  trip: TripResponse
  onArrive: () => void
  onSos: () => void
  onCancel: () => void
  isLoading: boolean
}

const STATUS_LABEL = {
  IN_PROGRESS: { label: '귀가 중',   color: 'var(--accent-blue)', bg: 'var(--grade-b-bg)' },
  ARRIVED:     { label: '도착 완료', color: 'var(--grade-a)',     bg: 'var(--grade-a-bg)' },
  SOS:         { label: 'SOS 발동',  color: 'var(--accent-red)',  bg: 'var(--accent-red-soft)' },
  CANCELLED:   { label: '취소됨',    color: 'var(--text-muted)',  bg: 'var(--bg-hover)' },
}

export default function TripStatusCard({ trip, onArrive, onSos, onCancel, isLoading }: Props) {
  const { label, color, bg } = STATUS_LABEL[trip.status]

  return (
    <div
      className="rounded-2xl p-6 flex flex-col gap-5"
      style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
    >
      <div className="flex items-center justify-between">
        <h2 className="font-display font-bold" style={{ color: 'var(--text-primary)' }}>귀가 현황</h2>
        <span
          className="text-xs font-semibold px-3 py-1 rounded-full flex items-center gap-1.5"
          style={{ color, background: bg }}
        >
          {trip.status === 'IN_PROGRESS' && <span className="beacon-dot" />}
          {label}
        </span>
      </div>

      <div className="flex flex-col gap-2 text-sm" style={{ color: 'var(--text-secondary)' }}>
        <div className="flex justify-between">
          <span>출발 시각</span>
          <span className="font-mono">{new Date(trip.departureAt).toLocaleTimeString('ko-KR')}</span>
        </div>
        <div className="flex justify-between">
          <span>예상 도착</span>
          <span className="font-mono font-medium" style={{ color: 'var(--text-primary)' }}>
            {new Date(trip.expectedArrivalAt).toLocaleTimeString('ko-KR')}
          </span>
        </div>
      </div>

      {trip.status === 'IN_PROGRESS' && (
        <div className="flex flex-col gap-3">
          <button
            onClick={onArrive}
            disabled={isLoading}
            className="w-full rounded-xl py-3 font-medium transition-colors disabled:opacity-50"
            style={{ background: 'var(--accent-green)', color: '#fff' }}
          >
            도착 완료
          </button>
          <button
            onClick={onSos}
            disabled={isLoading}
            className="w-full rounded-xl py-3 font-medium transition-colors disabled:opacity-50"
            style={{ background: 'var(--accent-red)', color: '#fff' }}
          >
            SOS 긴급 호출
          </button>
          <button
            onClick={onCancel}
            disabled={isLoading}
            className="w-full rounded-xl py-2.5 text-sm transition-colors disabled:opacity-50"
            style={{ color: 'var(--text-muted)', border: '1px solid var(--border)' }}
          >
            귀가 취소
          </button>
        </div>
      )}
    </div>
  )
}

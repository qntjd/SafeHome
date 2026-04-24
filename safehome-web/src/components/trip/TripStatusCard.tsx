import type { TripResponse } from '@/api/trip'

interface Props {
  trip: TripResponse
  onArrive: () => void
  onSos: () => void
  onCancel: () => void
  isLoading: boolean
}

const STATUS_LABEL = {
  IN_PROGRESS: { label: '귀가 중', color: 'text-blue-600 bg-blue-50' },
  ARRIVED:     { label: '도착 완료', color: 'text-green-600 bg-green-50' },
  SOS:         { label: 'SOS 발동', color: 'text-red-600 bg-red-50' },
  CANCELLED:   { label: '취소됨', color: 'text-gray-500 bg-gray-50' },
}

export default function TripStatusCard({ trip, onArrive, onSos, onCancel, isLoading }: Props) {
  const { label, color } = STATUS_LABEL[trip.status]

  return (
    <div className="bg-white rounded-2xl border border-gray-200 p-6 flex flex-col gap-5">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold text-gray-900">귀가 현황</h2>
        <span className={`text-xs font-semibold px-3 py-1 rounded-full ${color}`}>{label}</span>
      </div>

      <div className="flex flex-col gap-2 text-sm text-gray-600">
        <div className="flex justify-between">
          <span>출발 시각</span>
          <span>{new Date(trip.departureAt).toLocaleTimeString('ko-KR')}</span>
        </div>
        <div className="flex justify-between">
          <span>예상 도착</span>
          <span className="font-medium text-gray-900">
            {new Date(trip.expectedArrivalAt).toLocaleTimeString('ko-KR')}
          </span>
        </div>
      </div>

      {trip.status === 'IN_PROGRESS' && (
        <div className="flex flex-col gap-3">
          <button
            onClick={onArrive}
            disabled={isLoading}
            className="w-full bg-green-600 text-white rounded-xl py-3 font-medium hover:bg-green-700 disabled:opacity-50 transition-colors"
          >
            도착 완료
          </button>
          <button
            onClick={onSos}
            disabled={isLoading}
            className="w-full bg-red-600 text-white rounded-xl py-3 font-medium hover:bg-red-700 disabled:opacity-50 transition-colors"
          >
            SOS 긴급 호출
          </button>
          <button
            onClick={onCancel}
            disabled={isLoading}
            className="w-full text-gray-500 border border-gray-200 rounded-xl py-2.5 text-sm hover:bg-gray-50 transition-colors"
          >
            귀가 취소
          </button>
        </div>
      )}
    </div>
  )
}
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { contactApi } from '@/api/contact'
import { alertApi } from '@/api/alert'
import type { CreateContactRequest } from '@/api/contact'
import type { AlertHistoryResponse, SubscribeRequest } from '@/api/alert'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import Footer from '@/components/Footer'

declare global {
  interface Window { kakao: any }
}

const SIDO_LIST = [
  '서울특별시', '부산광역시', '대구광역시', '인천광역시',
  '광주광역시', '대전광역시', '울산광역시', '세종특별자치시',
  '경기도', '강원도', '충청북도', '충청남도',
  '전라북도', '전라남도', '경상북도', '경상남도', '제주특별자치도',
]

const LEVEL_CONFIG = {
  INFO:    { color: 'var(--accent-blue)',  bg: 'rgba(79,126,248,0.1)',  label: '일반' },
  WARNING: { color: 'var(--accent-amber)', bg: 'rgba(251,191,36,0.1)',  label: '주의' },
  DANGER:  { color: 'var(--accent-red)',   bg: 'rgba(248,113,113,0.1)', label: '위험' },
}

export default function SettingsPage() {
  const queryClient = useQueryClient()
  const { position } = useCurrentLocation()
  const [showForm, setShowForm]         = useState(false)
  const [alertTab, setAlertTab]         = useState<'전체' | '내 지역'>('전체')
  const [selectedSido, setSelectedSido] = useState('')
  const [sigungu, setSigungu]           = useState('')
  const [label, setLabel]               = useState('')

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } =
    useForm<CreateContactRequest>({ defaultValues: { notifyAfterMin: 10 } })

  // 비상연락처
  const { data: contactData, isLoading: contactLoading } = useQuery({
    queryKey: ['contacts'],
    queryFn: () => contactApi.getContacts(),
  })

  const addMutation = useMutation({
    mutationFn: (req: CreateContactRequest) => contactApi.addContact(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contacts'] })
      reset()
      setShowForm(false)
    },
    onError: (e: any) => alert(e.response?.data?.message ?? '추가 실패'),
  })

  const deleteContactMutation = useMutation({
    mutationFn: (id: string) => contactApi.deleteContact(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['contacts'] }),
  })

  // 알림 구독
  const { data: subsData } = useQuery({
    queryKey: ['subscriptions'],
    queryFn: () => alertApi.getSubscriptions(),
  })

  const { data: historyData } = useQuery({
    queryKey: ['alertHistory', alertTab],
    queryFn: () => alertTab === '내 지역'
      ? alertApi.getMyLocationHistory()
      : alertApi.getHistory(),
  })

  const subscribeMutation = useMutation({
    mutationFn: (req: SubscribeRequest) => alertApi.subscribe(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscriptions'] })
      setSelectedSido('')
      setSigungu('')
      setLabel('')
    },
    onError: (e: any) => alert(e.response?.data?.message ?? '구독 실패'),
  })

  const unsubscribeMutation = useMutation({
    mutationFn: (id: string) => alertApi.unsubscribe(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['subscriptions'] }),
  })

  const contacts      = contactData?.data?.data ?? []
  const subscriptions = subsData?.data?.data ?? []
  const alerts        = historyData?.data?.data ?? []

  const handleMyLocationSubscribe = () => {
    if (!window.kakao) return alert('카카오맵을 불러오는 중이에요.')
    const geocoder = new window.kakao.maps.services.Geocoder()
    geocoder.coord2Address(position.lng, position.lat, (result: any[], status: string) => {
      if (status === window.kakao.maps.services.Status.OK) {
        const addr  = result[0]?.address?.address_name ?? ''
        const parts = addr.split(' ')
        subscribeMutation.mutate({
          alertType:    'ALL',
          sidoName:     parts[0] ?? '',
          sigunguName:  parts[1] ?? '',
          label:        '내 위치',
          isMyLocation: true,
        })
      } else {
        alert('현재 위치를 확인할 수 없어요.')
      }
    })
  }

  const handleManualSubscribe = () => {
    if (!selectedSido) return alert('시도를 선택해주세요.')
    subscribeMutation.mutate({
      alertType:    'ALL',
      sidoName:     selectedSido,
      sigunguName:  sigungu || undefined,
      label:        label || undefined,
      isMyLocation: false,
    })
  }

  const cardStyle = {
    background:   'var(--bg-card)',
    border:       '1px solid var(--border)',
    borderRadius: 16,
    padding:      '1.25rem',
  }

  return (
    <div className="min-h-full" style={{ background: 'var(--bg-primary)' }}>
      <div className="max-w-2xl mx-auto px-4 py-6 sm:py-8 pb-24 sm:pb-8 flex flex-col gap-6">

        <div>
          <h1 className="text-xl sm:text-2xl font-semibold mb-1"
            style={{ color: 'var(--text-primary)' }}>설정</h1>
          <p className="text-sm" style={{ color: 'var(--text-muted)' }}>
            비상연락처 및 알림 설정
          </p>
        </div>

        {/* ── 비상연락처 ── */}
        <div style={cardStyle}>
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="font-medium text-sm" style={{ color: 'var(--text-primary)' }}>
                비상연락처
              </h2>
              <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
                미도착 시 자동으로 알림을 보낼 연락처예요 (최대 5명)
              </p>
            </div>
            {contacts.length < 5 && (
              <button
                onClick={() => setShowForm(!showForm)}
                className="text-xs px-3 py-1.5 rounded-lg font-medium transition-all"
                style={{ background: 'var(--accent-blue)', color: '#fff' }}
              >
                {showForm ? '취소' : '+ 추가'}
              </button>
            )}
          </div>

          {showForm && (
            <form
              onSubmit={handleSubmit((data) => addMutation.mutate(data))}
              className="rounded-xl p-4 mb-4 flex flex-col gap-3"
              style={{ background: 'var(--bg-hover)', border: '1px solid var(--border)' }}
            >
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs mb-1 block" style={{ color: 'var(--text-muted)' }}>이름</label>
                  <input
                    placeholder="홍길동"
                    className="w-full rounded-lg px-3 py-2 text-sm outline-none"
                    style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', color: 'var(--text-primary)' }}
                    {...register('name', { required: true })}
                  />
                  {errors.name && <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>필수 항목</p>}
                </div>
                <div>
                  <label className="text-xs mb-1 block" style={{ color: 'var(--text-muted)' }}>전화번호</label>
                  <input
                    placeholder="01012345678"
                    className="w-full rounded-lg px-3 py-2 text-sm outline-none"
                    style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', color: 'var(--text-primary)' }}
                    {...register('phone', { required: true })}
                  />
                  {errors.phone && (
                    <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                      {errors.phone.message ?? '필수 항목'}
                    </p>
                  )}
                </div>
              </div>
              <div>
                <label className="text-xs mb-1 block" style={{ color: 'var(--text-muted)' }}>미도착 알림 시간</label>
                <select
                  className="w-full rounded-lg px-3 py-2 text-sm outline-none"
                  style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', color: 'var(--text-primary)' }}
                  {...register('notifyAfterMin', { valueAsNumber: true })}
                >
                  <option value={5}>5분 후</option>
                  <option value={10}>10분 후</option>
                  <option value={15}>15분 후</option>
                  <option value={30}>30분 후</option>
                </select>
              </div>
              <button
                type="submit"
                disabled={isSubmitting || addMutation.isPending}
                className="w-full rounded-lg py-2 text-sm font-medium transition-all disabled:opacity-50"
                style={{ background: 'var(--accent-blue)', color: '#fff' }}
              >
                {addMutation.isPending ? '추가 중...' : '추가하기'}
              </button>
            </form>
          )}

          {contactLoading ? (
            <div className="flex flex-col gap-2">
              {[1, 2].map((i) => (
                <div key={i} className="h-14 rounded-xl animate-pulse" style={{ background: 'var(--bg-hover)' }} />
              ))}
            </div>
          ) : contacts.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-2xl mb-2">👥</p>
              <p className="text-sm" style={{ color: 'var(--text-muted)' }}>등록된 비상연락처가 없어요</p>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {contacts.map((contact) => (
                <div
                  key={contact.id}
                  className="flex items-center justify-between px-4 py-3 rounded-xl"
                  style={{ background: 'var(--bg-hover)' }}
                >
                  <div className="flex items-center gap-3">
                    <div
                      className="w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium"
                      style={{ background: 'rgba(79,126,248,0.15)', color: 'var(--accent-blue)' }}
                    >
                      {contact.name[0]}
                    </div>
                    <div>
                      <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>{contact.name}</p>
                      <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
                        {contact.phone} · {contact.notifyAfterMin}분 후 알림
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => deleteContactMutation.mutate(contact.id)}
                    disabled={deleteContactMutation.isPending}
                    className="text-xs px-3 py-1 rounded-lg transition-colors"
                    style={{ color: 'var(--accent-red)', border: '1px solid rgba(248,113,113,0.3)' }}
                  >
                    삭제
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* ── 알림 구독 ── */}
        <div style={cardStyle}>
          <h2 className="font-medium text-sm mb-1" style={{ color: 'var(--text-primary)' }}>
            재난·범죄 알림 구독
          </h2>
          <p className="text-xs mb-4" style={{ color: 'var(--text-muted)' }}>
            관심 지역의 재난·범죄 알림을 실시간으로 받아보세요
          </p>

          {/* 내 지역 자동 등록 */}
          <button
            onClick={handleMyLocationSubscribe}
            disabled={subscribeMutation.isPending}
            className="w-full rounded-xl py-2.5 text-sm font-medium transition-all disabled:opacity-50 mb-3"
            style={{ background: 'var(--accent-blue)', color: '#fff' }}
          >
            {subscribeMutation.isPending ? '등록 중...' : '📍 현재 위치로 내 지역 등록'}
          </button>

          {/* 직접 지역 선택 */}
          <div className="flex flex-col gap-2 mb-4">
            <div className="grid grid-cols-2 gap-2">
              <select
                value={selectedSido}
                onChange={e => setSelectedSido(e.target.value)}
                className="rounded-xl px-3 py-2.5 text-sm outline-none"
                style={{
                  background: 'var(--bg-hover)',
                  border: '1px solid var(--border)',
                  color: selectedSido ? 'var(--text-primary)' : 'var(--text-muted)',
                }}
              >
                <option value="">시도 선택</option>
                {SIDO_LIST.map(sido => (
                  <option key={sido} value={sido}>{sido}</option>
                ))}
              </select>
              <input
                value={sigungu}
                onChange={e => setSigungu(e.target.value)}
                placeholder="시군구 (선택)"
                className="rounded-xl px-3 py-2.5 text-sm outline-none"
                style={{
                  background: 'var(--bg-hover)',
                  border: '1px solid var(--border)',
                  color: 'var(--text-primary)',
                }}
              />
            </div>
            <input
              value={label}
              onChange={e => setLabel(e.target.value)}
              placeholder="별명 (예: 부모님댁, 직장)"
              className="w-full rounded-xl px-3 py-2.5 text-sm outline-none"
              style={{
                background: 'var(--bg-hover)',
                border: '1px solid var(--border)',
                color: 'var(--text-primary)',
              }}
            />
            <button
              onClick={handleManualSubscribe}
              disabled={subscribeMutation.isPending || !selectedSido}
              className="w-full rounded-xl py-2.5 text-sm font-medium transition-all disabled:opacity-50"
              style={{
                background: 'var(--bg-hover)',
                border: '1px solid var(--border)',
                color: 'var(--text-primary)',
              }}
            >
              + 관심 지역 추가
            </button>
          </div>

          {/* 구독 목록 — 내 지역 / 관심 지역 구분 */}
          {subscriptions.length > 0 && (
            <div className="flex flex-col gap-3 mb-4">
              {subscriptions.filter(s => s.isMyLocation).length > 0 && (
                <div>
                  <p className="text-xs font-medium mb-2" style={{ color: 'var(--accent-blue)' }}>
                    📍 내 지역
                  </p>
                  <div className="flex flex-col gap-2">
                    {subscriptions.filter(s => s.isMyLocation).map((sub) => (
                      <div
                        key={sub.id}
                        className="flex items-center justify-between px-3 py-2.5 rounded-xl"
                        style={{ background: 'rgba(79,126,248,0.08)', border: '1px solid rgba(79,126,248,0.2)' }}
                      >
                        <div>
                          <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                            {sub.label ?? sub.displayName}
                          </p>
                          <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
                            {sub.displayName}
                          </p>
                        </div>
                        <button
                          onClick={() => unsubscribeMutation.mutate(sub.id)}
                          className="text-xs px-3 py-1 rounded-lg"
                          style={{ color: 'var(--accent-red)', border: '1px solid rgba(248,113,113,0.3)' }}
                        >
                          해제
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {subscriptions.filter(s => !s.isMyLocation).length > 0 && (
                <div>
                  <p className="text-xs font-medium mb-2" style={{ color: 'var(--text-secondary)' }}>
                    🔔 관심 지역
                  </p>
                  <div className="flex flex-col gap-2">
                    {subscriptions.filter(s => !s.isMyLocation).map((sub) => (
                      <div
                        key={sub.id}
                        className="flex items-center justify-between px-3 py-2.5 rounded-xl"
                        style={{ background: 'var(--bg-hover)' }}
                      >
                        <div>
                          <p className="text-sm font-medium" style={{ color: 'var(--text-primary)' }}>
                            {sub.label ?? sub.displayName}
                          </p>
                          <p className="text-xs mt-0.5" style={{ color: 'var(--text-muted)' }}>
                            {sub.displayName}
                          </p>
                        </div>
                        <button
                          onClick={() => unsubscribeMutation.mutate(sub.id)}
                          className="text-xs px-3 py-1 rounded-lg"
                          style={{ color: 'var(--accent-red)', border: '1px solid rgba(248,113,113,0.3)' }}
                        >
                          해제
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* 알림 이력 */}
          <div style={{ borderTop: '1px solid var(--border)', paddingTop: 12 }}>
            <div className="flex gap-2 mb-3">
              {(['전체', '내 지역'] as const).map((tab) => (
                <button
                  key={tab}
                  onClick={() => setAlertTab(tab)}
                  className="text-xs px-3 py-1.5 rounded-full transition-all"
                  style={{
                    background: alertTab === tab ? 'var(--accent-blue)' : 'var(--bg-hover)',
                    color:      alertTab === tab ? '#fff' : 'var(--text-muted)',
                    border:     `1px solid ${alertTab === tab ? 'var(--accent-blue)' : 'var(--border)'}`,
                  }}
                >
                  {tab}
                </button>
              ))}
            </div>

            <p className="text-xs font-medium mb-2" style={{ color: 'var(--text-muted)' }}>
              최근 재난알림
            </p>
            {alerts.length === 0 ? (
              <p className="text-sm text-center py-6" style={{ color: 'var(--text-muted)' }}>
                최근 알림이 없습니다.
              </p>
            ) : (
              <div className="flex flex-col gap-2">
                {alerts.map((alert: AlertHistoryResponse) => {
                  const cfg = LEVEL_CONFIG[alert.level as keyof typeof LEVEL_CONFIG] ?? LEVEL_CONFIG.INFO
                  return (
                    <div
                      key={alert.id}
                      className="rounded-xl p-3"
                      style={{ background: cfg.bg, border: `1px solid ${cfg.color}30` }}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-xs font-semibold" style={{ color: cfg.color }}>
                          {alert.districtName} · {cfg.label}
                        </span>
                        <span className="text-xs" style={{ color: 'var(--text-muted)' }}>
                          {new Date(alert.issuedAt).toLocaleString('ko-KR')}
                        </span>
                      </div>
                      <p className="text-xs leading-relaxed" style={{ color: 'var(--text-secondary)' }}>
                        {alert.message}
                      </p>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </div>

      </div>
      <Footer />
    </div>
  )
}
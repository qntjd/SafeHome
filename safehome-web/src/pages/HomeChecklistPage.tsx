import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { DoorClosed, EyeOff, Flame, Users, Check, ShieldCheck } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { contactApi } from '@/api/contact'
import Footer from '@/components/Footer'

const STORAGE_KEY = 'safehome_checklist_v1'

interface ChecklistItem {
  id: string
  label: string
}

interface ChecklistCategory {
  key: string
  label: string
  icon: LucideIcon
  items: ChecklistItem[]
}

const CATEGORIES: ChecklistCategory[] = [
  {
    key: 'entry',
    label: '현관·출입',
    icon: DoorClosed,
    items: [
      { id: 'entry-lock',     label: '현관문 보조 잠금장치(체인·걸쇠)를 사용해요' },
      { id: 'entry-password', label: '도어락 비밀번호를 주기적으로 바꿔요' },
      { id: 'entry-cctv',     label: '현관 앞에 CCTV나 스마트 초인종이 있어요' },
      { id: 'entry-delivery', label: '택배는 실명 대신 별칭으로 받아요' },
    ],
  },
  {
    key: 'privacy',
    label: '창문·사생활 보호',
    icon: EyeOff,
    items: [
      { id: 'privacy-window-lock', label: '저층 창문에 잠금장치나 방범창이 있어요' },
      { id: 'privacy-window-shut', label: '외출 시 창문·베란다 문을 꼭 잠가요' },
      { id: 'privacy-curtain',     label: '커튼이나 블라인드로 내부가 안 보이게 해요' },
    ],
  },
  {
    key: 'fire',
    label: '화재·비상 대비',
    icon: Flame,
    items: [
      { id: 'fire-extinguisher', label: '집에 소화기가 있고 위치를 알아요' },
      { id: 'fire-alarm',        label: '화재경보기가 정상 작동해요' },
      { id: 'fire-outage',       label: '정전·가스 차단 시 대처 방법을 알아요' },
    ],
  },
  {
    key: 'habit',
    label: '생활 습관·비상연락',
    icon: Users,
    items: [
      { id: 'habit-timer',    label: '부재중에도 조명을 타이머로 켜둬요' },
      { id: 'habit-checkin',  label: '이웃이나 가족과 정기적으로 안부를 나눠요' },
    ],
  },
]

const TOTAL_MANUAL_ITEMS = CATEGORIES.reduce((sum, c) => sum + c.items.length, 0)
const TOTAL_ITEMS = TOTAL_MANUAL_ITEMS + 1 // +1 = 비상연락처 등록(자동 확인 항목)

const GRADE_CONFIG: Record<string, { color: string; bg: string; label: string }> = {
  A: { color: 'var(--grade-a)', bg: 'var(--grade-a-bg)', label: '아주 안심돼요' },
  B: { color: 'var(--grade-b)', bg: 'var(--grade-b-bg)', label: '안심할 수 있어요' },
  C: { color: 'var(--grade-c)', bg: 'var(--grade-c-bg)', label: '보통이에요' },
  D: { color: 'var(--grade-d)', bg: 'var(--grade-d-bg)', label: '보완이 필요해요' },
  F: { color: 'var(--grade-f)', bg: 'var(--grade-f-bg)', label: '점검이 시급해요' },
}

function scoreToGrade(percent: number): keyof typeof GRADE_CONFIG {
  if (percent >= 90) return 'A'
  if (percent >= 70) return 'B'
  if (percent >= 50) return 'C'
  if (percent >= 25) return 'D'
  return 'F'
}

function loadChecked(): Record<string, boolean> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

export default function HomeChecklistPage() {
  const [checked, setChecked] = useState<Record<string, boolean>>(loadChecked)

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(checked))
  }, [checked])

  const { data: contactsData } = useQuery({
    queryKey: ['contacts-checklist'],
    queryFn: () => contactApi.getContacts(),
  })
  const hasEmergencyContact = (contactsData?.data?.data?.length ?? 0) > 0

  const toggle = (id: string) => {
    setChecked(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const manualDone = CATEGORIES
    .flatMap(c => c.items)
    .filter(item => checked[item.id]).length
  const doneCount = manualDone + (hasEmergencyContact ? 1 : 0)
  const percent = Math.round((doneCount / TOTAL_ITEMS) * 100)
  const grade = scoreToGrade(percent)
  const gc = GRADE_CONFIG[grade]

  return (
    <div className="min-h-full pb-20 sm:pb-8" style={{ background: 'var(--bg-primary)' }}>

      {/* 헤더 배너 */}
      <div style={{ background: 'var(--ink)' }} className="px-4 sm:px-6 pt-7 pb-8 relative overflow-hidden">
        <div
          className="absolute rounded-full"
          style={{ width: 260, height: 260, right: -80, top: -120, background: 'radial-gradient(circle, rgba(232,163,61,0.16) 0%, transparent 70%)' }}
        />
        <div className="max-w-2xl mx-auto relative">
          <p className="text-sm mb-1.5 font-mono flex items-center gap-2" style={{ color: 'var(--ink-text-muted)' }}>
            <span className="beacon-dot" />
            SAFEHOME CHECKLIST
          </p>
          <h1 className="font-display font-black text-2xl sm:text-3xl mb-1.5" style={{ color: 'var(--ink-text)' }}>
            집 안심 체크리스트
          </h1>
          <p className="text-sm" style={{ color: 'var(--ink-text-muted)' }}>
            바깥보다 자주 있는 곳, 집 안의 안전도 함께 점검해요
          </p>
        </div>
      </div>

      <div className="max-w-2xl mx-auto px-4 sm:px-6 -mt-5">

        {/* 점수 카드 */}
        <div
          className="relative z-10 rounded-2xl p-5 mb-6 flex items-center gap-4"
          style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', boxShadow: 'var(--shadow-md)' }}
        >
          <div
            className="w-16 h-16 rounded-full flex items-center justify-center shrink-0 font-mono font-bold text-lg"
            style={{ background: gc.bg, color: gc.color }}
          >
            {percent}%
          </div>
          <div className="min-w-0">
            <p className="text-sm font-bold mb-0.5" style={{ color: gc.color }}>{gc.label}</p>
            <p className="text-xs" style={{ color: 'var(--text-muted)' }}>
              {TOTAL_ITEMS}개 항목 중 {doneCount}개 확인 완료
            </p>
            <div className="w-full h-1.5 rounded-full mt-2" style={{ background: 'var(--bg-primary)', minWidth: 160 }}>
              <div className="h-1.5 rounded-full transition-all" style={{ width: `${percent}%`, background: gc.color }} />
            </div>
          </div>
        </div>

        {/* 카테고리별 체크리스트 */}
        <div className="flex flex-col gap-4 mb-6">
          {CATEGORIES.map((cat) => {
            const CatIcon = cat.icon
            return (
              <div
                key={cat.key}
                className="rounded-2xl overflow-hidden"
                style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
              >
                <div className="flex items-center gap-2.5 px-4 py-3" style={{ borderBottom: '1px solid var(--border)' }}>
                  <CatIcon size={16} strokeWidth={2} style={{ color: 'var(--accent-blue)' }} />
                  <h2 className="font-display font-bold text-sm" style={{ color: 'var(--text-primary)' }}>
                    {cat.label}
                  </h2>
                </div>
                {cat.items.map((item, i) => {
                  const isChecked = !!checked[item.id]
                  return (
                    <button
                      key={item.id}
                      onClick={() => toggle(item.id)}
                      className="w-full flex items-center gap-3 px-4 py-3 text-left transition-colors"
                      style={{ borderTop: i > 0 ? '1px solid var(--border)' : undefined }}
                      onMouseEnter={e => (e.currentTarget.style.background = 'var(--bg-hover)')}
                      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
                    >
                      <span
                        className="w-5 h-5 rounded-full flex items-center justify-center shrink-0 transition-all"
                        style={{
                          background: isChecked ? 'var(--grade-a)' : 'transparent',
                          border: `1.5px solid ${isChecked ? 'var(--grade-a)' : 'var(--border-hover)'}`,
                        }}
                      >
                        {isChecked && <Check size={13} strokeWidth={3} color="#fff" />}
                      </span>
                      <span
                        className="text-sm"
                        style={{
                          color: isChecked ? 'var(--text-muted)' : 'var(--text-primary)',
                          textDecoration: isChecked ? 'line-through' : 'none',
                        }}
                      >
                        {item.label}
                      </span>
                    </button>
                  )
                })}
              </div>
            )
          })}

          {/* 자동 확인 항목 — 비상연락처 등록 여부 */}
          <div
            className="rounded-2xl overflow-hidden"
            style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
          >
            <div className="flex items-center gap-2.5 px-4 py-3" style={{ borderBottom: '1px solid var(--border)' }}>
              <ShieldCheck size={16} strokeWidth={2} style={{ color: 'var(--accent-blue)' }} />
              <h2 className="font-display font-bold text-sm" style={{ color: 'var(--text-primary)' }}>
                자동 확인
              </h2>
            </div>
            <div className="flex items-center gap-3 px-4 py-3">
              <span
                className="w-5 h-5 rounded-full flex items-center justify-center shrink-0"
                style={{
                  background: hasEmergencyContact ? 'var(--grade-a)' : 'transparent',
                  border: `1.5px solid ${hasEmergencyContact ? 'var(--grade-a)' : 'var(--border-hover)'}`,
                }}
              >
                {hasEmergencyContact && <Check size={13} strokeWidth={3} color="#fff" />}
              </span>
              <span className="text-sm flex-1" style={{ color: hasEmergencyContact ? 'var(--text-muted)' : 'var(--text-primary)' }}>
                비상연락처를 앱에 등록했어요
              </span>
              <span
                className="text-[11px] font-mono px-2 py-0.5 rounded-full shrink-0"
                style={{ background: 'var(--bg-hover)', color: 'var(--text-muted)' }}
              >
                실시간 확인
              </span>
            </div>
          </div>
        </div>

        <p className="text-xs text-center" style={{ color: 'var(--text-muted)' }}>
          체크 항목은 이 브라우저에만 저장돼요
        </p>

      </div>
      <Footer />
    </div>
  )
}

import { useState, useCallback, useEffect } from 'react'
import { useSosDetection } from '@/hooks/useSosDetection'
import { tripApi } from '@/api/trip'
import { useAuthStore } from '@/store/authStore'

interface Props {
  activeTripId?: string | null
}

export default function SosOverlay({ activeTripId }: Props) {
  const [sosMode, setSosMode]           = useState(false)
  const [countdown, setCountdown]       = useState(5)
  const [autoTriggered, setAutoTriggered] = useState(false)
  const [sending, setSending]           = useState(false)
  const [sent, setSent]                 = useState(false)
  const nickname = useAuthStore(s => s.nickname)

  const handleDetected = useCallback(() => {
    if (sosMode) return
    setAutoTriggered(true)
    setSosMode(true)
  }, [sosMode])

  const { isListening, transcript, isSupported, startListening, stopListening } =
    useSosDetection(handleDetected)

  // 자동 감지 시 5초 카운트다운 후 자동 신고
  useEffect(() => {
    if (!autoTriggered || sent) return
    if (countdown <= 0) {
      handleSos()
      return
    }
    const timer = setTimeout(() => setCountdown(c => c - 1), 1000)
    return () => clearTimeout(timer)
  }, [autoTriggered, countdown, sent])

  const handleSos = async () => {
    if (sending || sent) return
    setSending(true)

    try {
      if (activeTripId) {
        await tripApi.sos(activeTripId)
      }
    } catch (e) {
      console.error('[SOS] 전송 실패', e)
    } finally {
      setSending(false)
      setSent(true)
    }
  }

  const handleCancel = () => {
    setSosMode(false)
    setAutoTriggered(false)
    setCountdown(5)
    setSent(false)
  }

  const toggleListening = () => {
    if (isListening) stopListening()
    else startListening()
  }

  return (
    <>
      {/* SOS 모드 활성화 버튼 + 음성 감지 토글 */}
      <div className="fixed bottom-20 right-4 sm:bottom-6 flex flex-col gap-2 z-40">

        {/* 음성 감지 버튼 */}
        {isSupported && (
          <button
            onClick={toggleListening}
            className="w-12 h-12 rounded-full flex items-center justify-center shadow-lg transition-all"
            style={{
              background: isListening ? 'rgba(248,113,113,0.2)' : 'var(--bg-card)',
              border: `2px solid ${isListening ? 'var(--accent-red)' : 'var(--border)'}`,
            }}
            title={isListening ? '음성 감지 중지' : '음성 감지 시작'}
          >
            {isListening ? (
              <span className="animate-pulse text-lg">🎙</span>
            ) : (
              <span className="text-lg" style={{ opacity: 0.5 }}>🎙</span>
            )}
          </button>
        )}

        {/* SOS 버튼 */}
        <button
          onClick={() => setSosMode(true)}
          className="w-12 h-12 rounded-full flex items-center justify-center shadow-lg transition-all font-bold text-sm"
          style={{
            background: 'var(--accent-red)',
            color: '#fff',
            boxShadow: '0 0 20px rgba(248,113,113,0.4)',
          }}
        >
          SOS
        </button>
      </div>

      {/* 음성 감지 중 transcript 표시 */}
      {isListening && transcript && (
        <div
          className="fixed bottom-36 right-4 sm:bottom-24 rounded-xl px-3 py-2 text-xs max-w-48 z-40"
          style={{
            background: 'rgba(15,17,23,0.8)',
            border: '1px solid var(--border)',
            color: 'var(--text-muted)',
            backdropFilter: 'blur(8px)',
          }}
        >
          🎙 {transcript.slice(-30)}
        </div>
      )}

      {/* SOS 모드 오버레이 */}
      {sosMode && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          style={{ background: 'rgba(220,38,38,0.95)', backdropFilter: 'blur(4px)' }}
        >
          <div className="w-full max-w-sm text-center">

            {/* 아이콘 */}
            <div
              className="w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6"
              style={{ background: 'rgba(255,255,255,0.2)' }}
            >
              <span className="text-5xl">🚨</span>
            </div>

            {/* 자동 감지 vs 수동 */}
            {autoTriggered && !sent ? (
              <>
                <h2 className="text-2xl font-bold text-white mb-2">위험 감지!</h2>
                <p className="text-white/80 mb-2 text-sm">음성에서 위험 신호가 감지됐어요</p>
                <p className="text-white/60 text-xs mb-6">
                  "{transcript.slice(-20)}"
                </p>
                <div
                  className="w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-6 text-4xl font-bold text-white"
                  style={{ background: 'rgba(255,255,255,0.2)', border: '3px solid white' }}
                >
                  {countdown}
                </div>
                <p className="text-white/80 text-sm mb-6">
                  {countdown}초 후 자동으로 비상연락처에 알림이 전송돼요
                </p>
              </>
            ) : sent ? (
              <>
                <h2 className="text-2xl font-bold text-white mb-2">알림 전송 완료</h2>
                <p className="text-white/80 mb-6 text-sm">비상연락처에 현재 상황을 알렸어요</p>
              </>
            ) : (
              <>
                <h2 className="text-2xl font-bold text-white mb-2">위급상황이신가요?</h2>
                <p className="text-white/80 mb-6 text-sm">
                  아래 버튼을 누르면 비상연락처에<br />즉시 알림이 전송돼요
                </p>
              </>
            )}

            <div className="flex flex-col gap-3">

              {/* 112 신고 */}
              <a
                href="tel:112"
                className="w-full py-4 rounded-2xl font-bold text-lg transition-all"
                style={{ background: 'white', color: '#dc2626' }}
              >
                📞 112 신고
              </a>

              {/* 비상연락처 알림 */}
              {!sent && (
                <button
                  onClick={handleSos}
                  disabled={sending}
                  className="w-full py-3 rounded-2xl font-medium transition-all disabled:opacity-50"
                  style={{
                    background: 'rgba(255,255,255,0.2)',
                    border: '2px solid white',
                    color: 'white',
                  }}
                >
                  {sending ? '전송 중...' : '👥 비상연락처에 알림 보내기'}
                </button>
              )}

              {/* 취소 */}
              <button
                onClick={handleCancel}
                className="w-full py-3 rounded-2xl text-sm transition-all"
                style={{ color: 'rgba(255,255,255,0.6)' }}
              >
                {autoTriggered && !sent ? '취소 (오감지)' : '취소'}
              </button>
            </div>

            {/* 위치 공유 링크 */}
            <button
              onClick={() => {
                navigator.geolocation.getCurrentPosition(pos => {
                  const url = `https://map.kakao.com/link/map/현재위치,${pos.coords.latitude},${pos.coords.longitude}`
                  navigator.clipboard.writeText(url)
                  alert('위치 링크가 복사됐어요!')
                })
              }}
              className="mt-4 text-xs"
              style={{ color: 'rgba(255,255,255,0.5)' }}
            >
              📍 현재 위치 링크 복사
            </button>
          </div>
        </div>
      )}
    </>
  )
}
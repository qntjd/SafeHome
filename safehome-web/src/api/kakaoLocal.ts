import axios from 'axios'

const KAKAO_API_KEY = import.meta.env.VITE_KAKAO_API_KEY

export interface RegionInfo {
  sidoName: string
  sigunguName: string
}

export async function getRegionFromCoords(lat: number, lng: number): Promise<RegionInfo> {
  const response = await axios.get(
    'https://dapi.kakao.com/v2/local/geo/coord2regioncode.json',
    {
      headers: {
        Authorization: `KakaoAK ${KAKAO_API_KEY}`,
      },
      params: {
        x: lng, // 카카오 API는 경도(x)를 먼저 받음
        y: lat, // 위도(y)
      },
    }
  )

  // documents[0]이 보통 가장 정확한 법정동 기준 정보
  const region = response.data.documents[0]

  return {
    sidoName: region.region_1depth_name,   // 예: "대구광역시"
    sigunguName: region.region_2depth_name, // 예: "수성구"
  }
}
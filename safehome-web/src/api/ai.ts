import api from './axios'

export interface AiBriefingResponse {
    data: string
}

export const aiApi = {
    getBriefing: (districtCode: string, districtName: string, safetyScore: number, grade: string) =>
        api.get<AiBriefingResponse>('/ai/briefing', {
            params: { districtCode, districtName, safetyScore, grade },
        }),
}
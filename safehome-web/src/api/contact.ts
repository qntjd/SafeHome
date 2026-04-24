import api from './axios'

export interface EmergencyContact {
  id: string
  name: string
  phone: string
  notifyAfterMin: number
}

export interface CreateContactRequest {
  name: string
  phone: string
  notifyAfterMin: number
}

export const contactApi = {
  getContacts: () =>
    api.get<{ data: EmergencyContact[] }>('/contacts'),

  addContact: (data: CreateContactRequest) =>
    api.post<{ data: EmergencyContact }>('/contacts', data),

  deleteContact: (id: string) =>
    api.delete(`/contacts/${id}`),
}
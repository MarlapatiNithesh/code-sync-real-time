import axios from "axios"

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080"

export const apiClient = axios.create({
    baseURL: API_URL,
    headers: {
        "Content-Type": "application/json",
    },
})

apiClient.interceptors.request.use((config) => {
    const token = localStorage.getItem("authToken")
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

export interface AuthResponse {
    token: string
    userId: string
    email: string
    username: string
}

export interface UserProfile {
    id: string
    email: string
    username: string
}

export interface Room {
    id: string
    roomCode: string
    name: string
    ownerId: string
    ownerUsername: string
    createdAt: string
    updatedAt: string
}

export const signup = async (payload: {
    email: string
    username: string
    password: string
}) => {
    const { data } = await apiClient.post<AuthResponse>("/api/auth/signup", payload)
    return data
}

export const login = async (payload: { email: string; password: string }) => {
    const { data } = await apiClient.post<AuthResponse>("/api/auth/login", payload)
    return data
}

export const getProfile = async () => {
    const { data } = await apiClient.get<UserProfile>("/api/auth/me")
    return data
}

export const getMyRooms = async (page = 0, size = 10) => {
    const { data } = await apiClient.get<Room[]>(`/api/rooms?page=${page}&size=${size}`)
    return data
}

export const createRoom = async (name: string) => {
    const { data } = await apiClient.post<Room>("/api/rooms", { name })
    return data
}

export const getRoomByCode = async (roomCode: string) => {
    const { data } = await apiClient.get<Room>(`/api/rooms/code/${roomCode}`)
    return data
}

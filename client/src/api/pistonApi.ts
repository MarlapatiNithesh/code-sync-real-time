import axios, { AxiosInstance } from "axios"

const pistonBaseUrl = "/piston-api"

const instance: AxiosInstance = axios.create({
    baseURL: pistonBaseUrl,
    headers: {
        "Content-Type": "application/json",
    },
})

export default instance

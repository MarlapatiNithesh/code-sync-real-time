import express, { Request, Response } from "express"
import dotenv from "dotenv"
import http from "http"
import cors from "cors"
import { Server, Socket } from "socket.io"
import path from "path"
import rateLimit from "express-rate-limit"

import { SocketEvent, SocketId } from "./types/socket"
import { USER_CONNECTION_STATUS, User } from "./types/user"

dotenv.config()

const app = express()
const PORT = process.env.PORT || 3000

// Rate limiter for security
const limiter = rateLimit({
	windowMs: 15 * 60 * 1000, // 15 minutes
	max: 100, // limit each IP
	standardHeaders: true,
	legacyHeaders: false,
})
app.use(limiter)

app.use(express.json())

// Normalize client URL to remove trailing slash if present
const clientUrlRaw = process.env.CLIENT_URL || "*"
const clientUrl = clientUrlRaw.endsWith("/") ? clientUrlRaw.slice(0, -1) : clientUrlRaw

app.use(
	cors({
		origin: clientUrl,
		methods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
		credentials: true,
	})
)

// Serve static files
app.use(express.static(path.join(__dirname, "public")))

const server = http.createServer(app)

const io = new Server(server, {
	cors: {
		origin: clientUrl,
		methods: ["GET", "POST"],
		credentials: true,
	},
	maxHttpBufferSize: 1e8,
	pingTimeout: 60000,
})

let userSocketMap: User[] = []

function getUsersInRoom(roomId: string): User[] {
	return userSocketMap.filter((user) => user.roomId === roomId)
}

function getRoomId(socketId: SocketId): string | null {
	const user = userSocketMap.find((u) => u.socketId === socketId)
	if (!user) {
		console.error("Room ID is undefined for socket ID:", socketId)
		return null
	}
	return user.roomId
}

function getUserBySocketId(socketId: SocketId): User | null {
	const user = userSocketMap.find((u) => u.socketId === socketId)
	if (!user) {
		console.error("User not found for socket ID:", socketId)
		return null
	}
	return user
}

io.on("connection", (socket: Socket) => {
	console.log(`Socket connected: ${socket.id}`)

	socket.on(SocketEvent.JOIN_REQUEST, ({ roomId, username }) => {
		const isUsernameExist = getUsersInRoom(roomId).some(
			(u) => u.username === username
		)
		if (isUsernameExist) {
			io.to(socket.id).emit(SocketEvent.USERNAME_EXISTS)
			return
		}

		const user: User = {
			username,
			roomId,
			status: USER_CONNECTION_STATUS.ONLINE,
			cursorPosition: 0,
			typing: false,
			socketId: socket.id,
			currentFile: null,
		}

		userSocketMap.push(user)
		socket.join(roomId)

		socket.broadcast.to(roomId).emit(SocketEvent.USER_JOINED, { user })
		const users = getUsersInRoom(roomId)
		io.to(socket.id).emit(SocketEvent.JOIN_ACCEPTED, { user, users })
	})

	socket.on("disconnecting", () => {
		const user = getUserBySocketId(socket.id)
		if (!user) return
		const roomId = user.roomId
		socket.broadcast.to(roomId).emit(SocketEvent.USER_DISCONNECTED, { user })
		userSocketMap = userSocketMap.filter((u) => u.socketId !== socket.id)
	})

	// File and directory actions
	socket.on(SocketEvent.SYNC_FILE_STRUCTURE, ({ fileStructure, openFiles, activeFile, socketId }) => {
		io.to(socketId).emit(SocketEvent.SYNC_FILE_STRUCTURE, {
			fileStructure,
			openFiles,
			activeFile,
		})
	})

	socket.on(SocketEvent.DIRECTORY_CREATED, ({ parentDirId, newDirectory }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.DIRECTORY_CREATED, {
			parentDirId,
			newDirectory,
		})
	})

	socket.on(SocketEvent.DIRECTORY_UPDATED, ({ dirId, children }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.DIRECTORY_UPDATED, {
			dirId,
			children,
		})
	})

	socket.on(SocketEvent.DIRECTORY_RENAMED, ({ dirId, newName }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.DIRECTORY_RENAMED, {
			dirId,
			newName,
		})
	})

	socket.on(SocketEvent.DIRECTORY_DELETED, ({ dirId }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.DIRECTORY_DELETED, { dirId })
	})

	socket.on(SocketEvent.FILE_CREATED, ({ parentDirId, newFile }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.FILE_CREATED, {
			parentDirId,
			newFile,
		})
	})

	socket.on(SocketEvent.FILE_UPDATED, ({ fileId, newContent }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.FILE_UPDATED, {
			fileId,
			newContent,
		})
	})

	socket.on(SocketEvent.FILE_RENAMED, ({ fileId, newName }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.FILE_RENAMED, {
			fileId,
			newName,
		})
	})

	socket.on(SocketEvent.FILE_DELETED, ({ fileId }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.FILE_DELETED, { fileId })
	})

	// User status
	socket.on(SocketEvent.USER_OFFLINE, ({ socketId }) => {
		userSocketMap = userSocketMap.map((user) =>
			user.socketId === socketId
				? { ...user, status: USER_CONNECTION_STATUS.OFFLINE }
				: user
		)
		const roomId = getRoomId(socketId)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.USER_OFFLINE, { socketId })
	})

	socket.on(SocketEvent.USER_ONLINE, ({ socketId }) => {
		userSocketMap = userSocketMap.map((user) =>
			user.socketId === socketId
				? { ...user, status: USER_CONNECTION_STATUS.ONLINE }
				: user
		)
		const roomId = getRoomId(socketId)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.USER_ONLINE, { socketId })
	})

	// Chat
	socket.on(SocketEvent.SEND_MESSAGE, ({ message }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.RECEIVE_MESSAGE, { message })
	})

	// Cursor typing
	socket.on(SocketEvent.TYPING_START, ({ cursorPosition }) => {
		userSocketMap = userSocketMap.map((user) =>
			user.socketId === socket.id
				? { ...user, typing: true, cursorPosition }
				: user
		)
		const user = getUserBySocketId(socket.id)
		if (!user) return
		socket.broadcast.to(user.roomId).emit(SocketEvent.TYPING_START, { user })
	})

	socket.on(SocketEvent.TYPING_PAUSE, () => {
		userSocketMap = userSocketMap.map((user) =>
			user.socketId === socket.id ? { ...user, typing: false } : user
		)
		const user = getUserBySocketId(socket.id)
		if (!user) return
		socket.broadcast.to(user.roomId).emit(SocketEvent.TYPING_PAUSE, { user })
	})

	// Drawing
	socket.on(SocketEvent.REQUEST_DRAWING, () => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.REQUEST_DRAWING, {
			socketId: socket.id,
		})
	})

	socket.on(SocketEvent.SYNC_DRAWING, ({ drawingData, socketId }) => {
		socket.broadcast.to(socketId).emit(SocketEvent.SYNC_DRAWING, { drawingData })
	})

	socket.on(SocketEvent.DRAWING_UPDATE, ({ snapshot }) => {
		const roomId = getRoomId(socket.id)
		if (!roomId) return
		socket.broadcast.to(roomId).emit(SocketEvent.DRAWING_UPDATE, { snapshot })
	})

	socket.on("error", (err) => {
		console.error("Socket error:", err)
	})
})

app.get("/", (req: Request, res: Response) => {
	res.sendFile(path.join(__dirname, "..", "public", "index.html"))
})

server.listen(PORT, () => {
	console.log(`✅ Server running on http://localhost:${PORT}`)
})

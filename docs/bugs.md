debug:

[WatchPage] Connecting WebSocket - StreamID: 2cf9aa77-5032-4246-bb8e-21303f00d7f1, ViewerID: viewer_1770144862611_65awmhe56
WatchPage.tsx:56 [WatchPage] Component unmount - Sending viewer_left
websocketService.ts:65 WebSocket disconnecting (ref count reached 0)
WatchPage.tsx:126 [WatchPage] Connecting WebSocket - StreamID: 2cf9aa77-5032-4246-bb8e-21303f00d7f1, ViewerID: viewer_1770144862611_65awmhe56
apiService.ts:38 [API] GET /streams/2cf9aa77-5032-4246-bb8e-21303f00d7f1
websocketService.ts:32 [WebSocket] Client has been marked inactive, will not attempt to connect
apiService.ts:38 [API] GET /streams/2cf9aa77-5032-4246-bb8e-21303f00d7f1
websocketService.ts:32 [WebSocket] Opening Web Socket...
2apiService.ts:54 [API] GET /streams/2cf9aa77-5032-4246-bb8e-21303f00d7f1 - 200
logger.ts:26 [DEBUG] Checking HLS availability... 
websocketService.ts:32 [WebSocket] Web Socket Opened...
websocketService.ts:32 [WebSocket] >>> CONNECT
accept-version:1.2,1.1,1.0
heart-beat:4000,4000


logger.ts:23 [INFO] HLS is available! 
websocketService.ts:32 [WebSocket] Received data
websocketService.ts:32 [WebSocket] <<< CONNECTED
heart-beat:0,0
version:1.2
content-length:0


websocketService.ts:32 [WebSocket] connected to server undefined
websocketService.ts:39 WebSocket connected
websocketService.ts:32 [WebSocket] >>> SUBSCRIBE
id:sub-0
destination:/topic/stream/2cf9aa77-5032-4246-bb8e-21303f00d7f1/status


websocketService.ts:32 [WebSocket] >>> SUBSCRIBE
id:sub-1
destination:/topic/stream/2cf9aa77-5032-4246-bb8e-21303f00d7f1/viewers


WatchPage.tsx:168 [WatchPage] Sending viewer_joined - StreamID: 2cf9aa77-5032-4246-bb8e-21303f00d7f1, ViewerID: viewer_1770144862611_65awmhe56
websocketService.ts:32 [WebSocket] >>> SEND
destination:/app/stream/2cf9aa77-5032-4246-bb8e-21303f00d7f1/join
content-length:45


useStreamingTime.ts:50 [DEBUG] clientNow: 1770150952902
useStreamingTime.ts:51 [DEBUG] serverTimestamp: 1770150952877
useStreamingTime.ts:52 [DEBUG] serverClientDiff: 25
useStreamingTime.ts:53 [DEBUG] adjustedClientTime: 1770150952877
useStreamingTime.ts:54 [DEBUG] startTime: 1770161730548
useStreamingTime.ts:55 [DEBUG] diff: -10777671
useStreamingTime.ts:58 [DEBUG] Diff <= 0, setting 00:00:00
useStreamingTime.ts:50 [DEBUG] clientNow: 1770150952902
useStreamingTime.ts:51 [DEBUG] serverTimestamp: 1770150952877
useStreamingTime.ts:52 [DEBUG] serverClientDiff: 25
useStreamingTime.ts:53 [DEBUG] adjustedClientTime: 1770150952877
useStreamingTime.ts:54 [DEBUG] startTime: 1770161730548
useStreamingTime.ts:55 [DEBUG] diff: -10777671
useStreamingTime.ts:58 [DEBUG] Diff <= 0, setting 00:00:00
useStreamingTime.ts:50 [DEBUG] clientNow: 1770150952902
useStreamingTime.ts:51 [DEBUG] serverTimestamp: 1770150952877
useStreamingTime.ts:52 [DEBUG] serverClientDiff: 25
useStreamingTime.ts:53 [DEBUG] adjustedClientTime: 1770150952877
useStreamingTime.ts:54 [DEBUG] startTime: 1770161730548
useStreamingTime.ts:55 [DEBUG] diff: -10777671
useStreamingTime.ts:58 [DEBUG] Diff <= 0, setting 00:00:00
useStreamingTime.ts:50 [DEBUG] clientNow: 1770150952902
useStreamingTime.ts:51 [DEBUG] serverTimestamp: 1770150952877
useStreamingTime.ts:52 [DEBUG] serverClientDiff: 25
useStreamingTime.ts:53 [DEBUG] adjustedClientTime: 1770150952877
useStreamingTime.ts:54 [DEBUG] startTime: 1770161730548
useStreamingTime.ts:55 [DEBUG] diff: -10777671
useStreamingTime.ts:58 [DEBUG] Diff <= 0, setting 00:00:00
logger.ts:23 [INFO] HLS manifest parsed successfully 
websocketService.ts:32 [WebSocket] Received data
websocketService.ts:32 [WebSocket] <<< MESSAGE
content-length:60
message-id:lx3x21xb-223
subscription:sub-1
content-type:application/json
destination:/topic/stream/2cf9aa77-5032-4246-bb8e-21303f00d7f1/viewers
content-length:60


blob:http://localhost:3001/cc168a78-d9d3-4f75-bdc3-b364cfbe0a43:1  GET blob:http://localhost:3001/cc168a78-d9d3-4f75-bdc3-b364cfbe0a43 net::ERR_FILE_NOT_FOUND
useStreamingTime.ts:50 [DEBUG] clientNow: 1770150953902
useStreamingTime.ts:51 [DEBUG] serverTimestamp: 1770150952877
useStreamingTime.ts:52 [DEBUG] serverClientDiff: 25
useStreamingTime.ts:53 [DEBUG] adjustedClientTime: 1770150953877
useStreamingTime.ts:54 [DEBUG] startTime: 1770161730548
useStreamingTime.ts:55 [DEBUG] diff: -10776671
useStreamingTime.ts:58 [DEBUG] Diff <= 0, setting 00:00:00
useStreamingTime.ts:50 [DEBUG] clientNow: 1770150953903
useStreamingTime.ts:51 [DEBUG] serverTimestamp: 1770150952877
useStreamingTime.ts:52 [DEBUG] serverClientDiff: 25
useStreamingTime.ts:53 [DEBUG] adjustedClientTime: 1770150953878
useStreamingTime.ts:54 [DEBUG] startTime: 1770161730548
useStreamingTime.ts:55 [DEBUG] diff: -10776670
useStreamingTime.ts:58 [DEBUG] Diff <= 0, setting 00:00:00
useStreamingTime.ts:50 [DEBUG] clientNow: 1770150953903
useStreamingTime.ts:51 [DEBUG] serverTimestamp: 1770150952877
useStreamingTime.ts:52 [DEBUG] serverClientDiff: 25
useStreamingTime.ts:53 [DEBUG] adjustedClientTime: 1770150953878
useStreamingTime.ts:54 [DEBUG] startTime: 1770161730548
useStreamingTime.ts:55 [DEBUG] diff: -10776670
useStreamingTime.ts:58 [DEBUG] Diff <= 0, setting 00:00:00
useStreamingTime.ts:50 [DEBUG] clientNow: 1770150953903
useStreamingTime.ts:51 [DEBUG] serverTimestamp: 1770150952877
useStreamingTime.ts:52 [DEBUG] serverClientDiff: 25
useStreamingTime.ts:53 [DEBUG] adjustedClientTime: 1770150953878
useStreamingTime.ts:54 [DEBUG] startTime: 1770161730548
useStreamingTime.ts:55 [DEBUG] diff: -10776670
useStreamingTime.ts:58 [DEBUG] Diff <= 0, setting 00:00:00
useStreamingTime.ts:50 [DEBUG] clientNow: 1770150954902
useStreamingTime.ts:51 [DEBUG] serverTimestamp: 1770150952877
useStreamingTime.ts:52 [DEBUG] serverClientDiff: 25
useStreamingTime.ts:53 [DEBUG] adjustedClientTime: 1770150954877
useStreamingTime.ts:54 [DEBUG] startTime: 1770161730548
useStreamingTime.ts:55 [DEBUG] diff: -10775671
useStreamingTime.ts:58 [DEBUG] Diff <= 0, setting 00:00:00

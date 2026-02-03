import { lazy, Suspense } from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";

// Create query client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 30, // 30 seconds
      refetchInterval: 1000 * 30, // Auto-refetch every 30 seconds
      refetchOnWindowFocus: true,
      retry: 1,
    },
  },
});

// Lazy load pages for code splitting
const HomePage = lazy(() =>
  import("./features/stream/pages/HomePage").then((m) => ({
    default: m.HomePage,
  })),
);
const MyStreamsPage = lazy(() =>
  import("./features/stream/pages/MyStreamsPage").then((m) => ({
    default: m.MyStreamsPage,
  })),
);
const StreamerDashboard = lazy(() =>
  import("./features/stream/pages/StreamerDashboard").then((m) => ({
    default: m.StreamerDashboard,
  })),
);
const WatchPage = lazy(() =>
  import("./features/stream/pages/WatchPage").then((m) => ({
    default: m.WatchPage,
  })),
);

// Loading fallback component
function LoadingFallback() {
  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center">
      <Loader2 className="w-8 h-8 text-purple-500 animate-spin" />
    </div>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Suspense fallback={<LoadingFallback />}>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/my-streams" element={<MyStreamsPage />} />
            <Route
              path="/dashboard/:streamId"
              element={<StreamerDashboard />}
            />
            <Route path="/watch/:streamId" element={<WatchPage />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;

const resolveApiBaseUrl = () => {
    const envUrl = import.meta.env?.VITE_API_URL;
    if (envUrl && envUrl.trim() !== '') {
        return envUrl.trim().replace(/\/+$/, '');
    }

    if (typeof window !== 'undefined' && window.location) {
        const { protocol, hostname } = window.location;
        const backendPort = import.meta.env?.VITE_BACKEND_PORT || '8080';
        return `${protocol}//${hostname}:${backendPort}`;
    }

    return 'http://localhost:8080';
};

export const API_BASE_URL = resolveApiBaseUrl();

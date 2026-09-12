FROM nginx:1.27-alpine

COPY index.html docs.html /usr/share/nginx/html/
COPY assets /usr/share/nginx/html/assets
COPY scripts /usr/share/nginx/html/scripts
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1

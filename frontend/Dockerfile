####
# Dockerfile multi-stage do frontend (React + Vite).
#
# Estágio 1 ("build"): imagem Node.js só pra rodar `npm ci && npm run build`
# e gerar os arquivos estáticos em dist/ — essa imagem NÃO vai pra produção,
# serve só de "oficina" temporária.
#
# Estágio 2 (final): imagem nginx certificada da Red Hat (mesma família UBI
# do backend), que só serve os arquivos estáticos gerados no estágio 1 —
# nenhum Node.js sobra na imagem final, ela fica pequena e sem ferramentas
# de build desnecessárias.
#
# Build local antes de usar este Dockerfile no OpenShift:
#   cd frontend
#   npm ci
#   npm run build       # gera dist/, já lendo frontend/.env.production
#
# Build com Docker/Podman local (opcional, pra testar):
#   docker build -t stockinteligence-frontend .
#   docker run -p 8080:8080 stockinteligence-frontend
###

FROM registry.access.redhat.com/ubi9/nodejs-20:latest AS build
WORKDIR /opt/app-root/src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# .env.production já commitado no repo com a URL pública do backend —
# o Vite carrega esse arquivo automaticamente em modo produção.
RUN npm run build

FROM registry.access.redhat.com/ubi9/nginx-124:latest
# --chown=1001:0 (usuário padrão da imagem nginx UBI, grupo 0) garante que
# o conteúdo copiado já nasce com a permissão certa pro UID arbitrário que
# o OpenShift vai atribuir em runtime (mesmo raciocínio da imagem do
# Postgres — ver infra/openshift/README.md § 2).
COPY --from=build --chown=1001:0 /opt/app-root/src/dist /opt/app-root/src

# Snippet de fallback do React Router (try_files) — copiado pro caminho de
# config que o server block default do nginx já inclui automaticamente
# (variável de ambiente definida pela própria imagem base).
COPY --chown=1001:0 nginx-default-cfg/spa-fallback.conf /tmp/spa-fallback.conf
RUN cp /tmp/spa-fallback.conf "${NGINX_DEFAULT_CONF_PATH}/spa-fallback.conf"

EXPOSE 8080
# A imagem base, quando usada fora do fluxo `s2i build` (nosso caso, com
# Dockerfile/COPY comum), tem como comando padrão um script que só imprime
# instruções de uso — não inicia o nginx sozinha. Precisa sobrescrever o
# CMD explicitamente apontando pro script real de start.
CMD ["/usr/libexec/s2i/run"]

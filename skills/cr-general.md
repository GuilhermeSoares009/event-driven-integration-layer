# Skill: Code Review (Geral)

## Objetivo

Avaliar qualidade, corretude e risco de mudancas com foco em seguranca e manutencao.

## Processo

1. Entender o escopo e o motivo da mudanca.
2. Ler o diff por completo.
3. Validar comportamento esperado, incluindo casos limite.
4. Conferir testes e cobertura relevante.
5. Documentar riscos e sugestoes objetivas.

## Checklist

- **Corretude**: atende requisitos e nao altera comportamento indevidamente
- **Erro/Edge cases**: falhas previsiveis tratadas, mensagens claras
- **Seguranca**: validacao de entrada, nao expor segredos
- **Performance**: evita hot paths desnecessarios e N+1
- **Confiabilidade**: retry, idempotencia, timeouts quando aplicavel
- **Observabilidade**: logs e metricas quando necessario
- **Testes**: unit/integration relevantes e deterministas
- **Docs**: README/ADR atualizados se necessario

## Saida esperada

- Resumo curto do que foi revisado
- Lista de riscos ou bloqueios
- Sugestoes objetivas (com arquivo/linha quando possivel)

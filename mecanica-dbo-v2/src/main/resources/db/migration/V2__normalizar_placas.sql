-- ============================================================
--  V2 — placas só com letras e números, em maiúsculas
--
--  Antes, "JEH-3030" e "JEH3030" podiam ser gravados como carros
--  diferentes. Agora a placa fica sempre sem hífen/espaço (o hífen do
--  padrão antigo é só exibição: Placas.formatar no backend e o pipe
--  "placa" no front).
--
--  Em 29/10/2026 isto alterava 1 linha no banco da oficina:
--  veiculo id 1, JEH-3030 → JEH3030. Backup feito antes.
-- ============================================================

-- 1. Se duas placas ficarem iguais depois de normalizar, PARA sem mudar nada.
--    (O Flyway desfaz a migração inteira e o sistema não sobe, com esta mensagem no log.)
DO $$
DECLARE
    colisoes text;
BEGIN
    SELECT string_agg(normalizada || ' (' || placas || ')', '; ')
    INTO   colisoes
    FROM (
        SELECT upper(regexp_replace(placa, '[^A-Za-z0-9]', '', 'g')) AS normalizada,
               string_agg('id ' || id || ': ' || placa, ', ' ORDER BY id) AS placas
        FROM   veiculo
        GROUP  BY 1
        HAVING count(*) > 1
    ) x;

    IF colisoes IS NOT NULL THEN
        RAISE EXCEPTION 'Normalização de placas cancelada. Estas placas ficariam iguais: %. Junte ou corrija esses veículos à mão e suba o sistema de novo.', colisoes;
    END IF;
END $$;

-- 2. Normaliza.
UPDATE veiculo
SET    placa = upper(regexp_replace(placa, '[^A-Za-z0-9]', '', 'g'))
WHERE  placa <> upper(regexp_replace(placa, '[^A-Za-z0-9]', '', 'g'));

-- 3. Trava: daqui em diante, nenhuma placa com hífen, espaço ou minúscula
--    entra, nem por edição direta no banco.
ALTER TABLE veiculo
    ADD CONSTRAINT veiculo_placa_formato_check CHECK (placa ~ '^[A-Z0-9]+$');

COMMENT ON COLUMN veiculo.placa IS 'Só letras e números, maiúsculas (JEH3030, ABC1D23). O hífen do padrão antigo é só exibição.';

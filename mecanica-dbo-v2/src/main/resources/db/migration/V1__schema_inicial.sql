-- ============================================================
--  Mecânica DBO — schema inicial (Flyway V1)
--
--  Gerado em 29/10/2026 a partir do banco real mecanica_dbo
--  (PostgreSQL 18.4) com:
--    pg_dump --schema-only --no-owner --no-privileges -d mecanica_dbo
--
--  Limpeza feita para o Flyway: removidas as linhas \restrict/\unrestrict
--  (meta-comandos do psql), o set_config('search_path', ...) e o
--  SET transaction_timeout (só existe a partir do PostgreSQL 17).
--
--  Em banco que JÁ tem as tabelas, este arquivo NÃO roda: o Flyway faz
--  baseline na versão 1 (spring.flyway.baseline-on-migrate). Ele só roda
--  em banco vazio, para recriar a estrutura do zero.
--
--  NUNCA edite este arquivo depois de aplicado. Mudança de estrutura nova
--  vai num arquivo novo: V2__descricao.sql, V3__..., etc.
-- ============================================================

--
-- PostgreSQL database dump
--


-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: status_ocr; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.status_ocr AS ENUM (
    'PENDENTE',
    'PROCESSADO',
    'ERRO'
);


--
-- Name: status_os; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.status_os AS ENUM (
    'ABERTA',
    'EM_ANDAMENTO',
    'CONCLUIDA',
    'ENTREGUE'
);


--
-- Name: recalcular_totais_os(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.recalcular_totais_os() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_os_id     BIGINT;
    v_total_p   DECIMAL(10,2);
    v_total_s   DECIMAL(10,2);
BEGIN
    v_os_id := COALESCE(NEW.os_id, OLD.os_id);

    -- Peças pagas pelo cliente ficam fora do total cobrado.
    SELECT COALESCE(SUM(valor_total), 0)
    INTO   v_total_p
    FROM   item_peca
    WHERE  os_id = v_os_id
      AND  pago_pelo_cliente = FALSE;

    SELECT COALESCE(SUM(valor), 0)
    INTO   v_total_s
    FROM   item_servico
    WHERE  os_id = v_os_id;

    UPDATE ordem_servico SET
        total_pecas    = v_total_p,
        total_servicos = v_total_s,
        total_geral    = v_total_p + v_total_s
    WHERE id = v_os_id;

    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: cliente; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cliente (
    id bigint NOT NULL,
    nome character varying(120) NOT NULL,
    cpf character varying(14),
    telefone_1 character varying(20) NOT NULL,
    telefone_2 character varying(20),
    email character varying(100),
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE cliente; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.cliente IS 'Proprietários dos veículos atendidos';


--
-- Name: COLUMN cliente.cpf; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cliente.cpf IS 'Formato: xxx.xxx.xxx-xx (opcional)';


--
-- Name: COLUMN cliente.ativo; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.cliente.ativo IS 'FALSE = inativo (sem exclusão física)';


--
-- Name: cliente_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cliente_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cliente_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cliente_id_seq OWNED BY public.cliente.id;


--
-- Name: item_peca; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.item_peca (
    id bigint NOT NULL,
    os_id bigint NOT NULL,
    nf_id bigint,
    descricao character varying(200) NOT NULL,
    quantidade numeric(8,2) DEFAULT 1 NOT NULL,
    valor_unitario numeric(10,2) NOT NULL,
    valor_total numeric(10,2) GENERATED ALWAYS AS (round((quantidade * valor_unitario), 2)) STORED,
    pago_pelo_cliente boolean DEFAULT false NOT NULL,
    criado_em timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE item_peca; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.item_peca IS 'Peças utilizadas em cada OS';


--
-- Name: COLUMN item_peca.nf_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.item_peca.nf_id IS 'Preenchido quando o item veio de importação OCR';


--
-- Name: COLUMN item_peca.valor_unitario; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.item_peca.valor_unitario IS 'Pode ser negativo (linha de crédito)';


--
-- Name: COLUMN item_peca.valor_total; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.item_peca.valor_total IS 'Calculado pelo banco: quantidade × valor_unitario';


--
-- Name: COLUMN item_peca.pago_pelo_cliente; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.item_peca.pago_pelo_cliente IS 'TRUE = peça paga pelo cliente; não entra no total cobrado';


--
-- Name: item_peca_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.item_peca_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: item_peca_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.item_peca_id_seq OWNED BY public.item_peca.id;


--
-- Name: item_servico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.item_servico (
    id bigint NOT NULL,
    os_id bigint NOT NULL,
    descricao character varying(200) NOT NULL,
    valor numeric(10,2) NOT NULL,
    criado_em timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT item_servico_valor_check CHECK ((valor >= (0)::numeric))
);


--
-- Name: TABLE item_servico; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.item_servico IS 'Serviços de mão de obra cobrados em cada OS';


--
-- Name: item_servico_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.item_servico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: item_servico_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.item_servico_id_seq OWNED BY public.item_servico.id;


--
-- Name: nota_fiscal; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.nota_fiscal (
    id bigint NOT NULL,
    os_id bigint NOT NULL,
    fornecedor character varying(120),
    cnpj_fornecedor character varying(18),
    numero_nf character varying(30),
    data_emissao date,
    valor_total_nf numeric(10,2),
    imagem_path character varying(300),
    ocr_status public.status_ocr DEFAULT 'PENDENTE'::public.status_ocr NOT NULL,
    importado_em timestamp without time zone,
    criado_em timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE nota_fiscal; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.nota_fiscal IS 'Notas fiscais de fornecedores, lidas por OCR';


--
-- Name: COLUMN nota_fiscal.imagem_path; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.nota_fiscal.imagem_path IS 'Caminho local da foto da NF';


--
-- Name: COLUMN nota_fiscal.ocr_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.nota_fiscal.ocr_status IS 'PENDENTE = aguardando leitura OCR';


--
-- Name: nota_fiscal_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.nota_fiscal_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: nota_fiscal_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.nota_fiscal_id_seq OWNED BY public.nota_fiscal.id;


--
-- Name: ordem_servico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ordem_servico (
    id bigint NOT NULL,
    veiculo_id bigint NOT NULL,
    km_entrada integer,
    data_entrada date DEFAULT CURRENT_DATE NOT NULL,
    data_saida_prevista date,
    data_saida_real date,
    status public.status_os DEFAULT 'ABERTA'::public.status_os NOT NULL,
    reclamacoes text,
    diagnostico text,
    total_pecas numeric(10,2) DEFAULT 0.00 NOT NULL,
    total_servicos numeric(10,2) DEFAULT 0.00 NOT NULL,
    total_geral numeric(10,2) DEFAULT 0.00 NOT NULL,
    criado_em timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_datas CHECK (((data_saida_prevista IS NULL) OR (data_saida_prevista >= data_entrada))),
    CONSTRAINT ordem_servico_km_entrada_check CHECK ((km_entrada >= 0))
);


--
-- Name: TABLE ordem_servico; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.ordem_servico IS 'Ordens de serviço (recibos) da oficina';


--
-- Name: COLUMN ordem_servico.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ordem_servico.status IS 'ABERTA → EM_ANDAMENTO → CONCLUIDA → ENTREGUE';


--
-- Name: COLUMN ordem_servico.total_geral; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ordem_servico.total_geral IS 'total_pecas + total_servicos (calculado pela trigger)';


--
-- Name: ordem_servico_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ordem_servico_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ordem_servico_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.ordem_servico_id_seq OWNED BY public.ordem_servico.id;


--
-- Name: veiculo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.veiculo (
    id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    placa character varying(10) NOT NULL,
    marca character varying(60) NOT NULL,
    modelo character varying(80) NOT NULL,
    chassi character varying(20),
    cor character varying(40) NOT NULL,
    combustivel character varying(20) NOT NULL,
    ano_fabricacao smallint,
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp without time zone DEFAULT now() NOT NULL,
    CONSTRAINT veiculo_ano_fabricacao_check CHECK (((ano_fabricacao >= 1950) AND (ano_fabricacao <= 2100))),
    CONSTRAINT veiculo_combustivel_check CHECK (((combustivel)::text = ANY ((ARRAY['GASOLINA'::character varying, 'ETANOL'::character varying, 'FLEX'::character varying, 'DIESEL'::character varying, 'ELETRICO'::character varying, 'HIBRIDO'::character varying, 'GNV'::character varying])::text[])))
);


--
-- Name: TABLE veiculo; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.veiculo IS 'Veículos vinculados a clientes';


--
-- Name: COLUMN veiculo.cliente_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.veiculo.cliente_id IS 'FK → dono do veículo';


--
-- Name: COLUMN veiculo.placa; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.veiculo.placa IS 'Formato antigo (AAA-0000) ou Mercosul (AAA0A00)';


--
-- Name: veiculo_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.veiculo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: veiculo_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.veiculo_id_seq OWNED BY public.veiculo.id;


--
-- Name: cliente id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cliente ALTER COLUMN id SET DEFAULT nextval('public.cliente_id_seq'::regclass);


--
-- Name: item_peca id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_peca ALTER COLUMN id SET DEFAULT nextval('public.item_peca_id_seq'::regclass);


--
-- Name: item_servico id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_servico ALTER COLUMN id SET DEFAULT nextval('public.item_servico_id_seq'::regclass);


--
-- Name: nota_fiscal id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nota_fiscal ALTER COLUMN id SET DEFAULT nextval('public.nota_fiscal_id_seq'::regclass);


--
-- Name: ordem_servico id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordem_servico ALTER COLUMN id SET DEFAULT nextval('public.ordem_servico_id_seq'::regclass);


--
-- Name: veiculo id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.veiculo ALTER COLUMN id SET DEFAULT nextval('public.veiculo_id_seq'::regclass);


--
-- Name: cliente cliente_cpf_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cliente
    ADD CONSTRAINT cliente_cpf_key UNIQUE (cpf);


--
-- Name: cliente cliente_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cliente
    ADD CONSTRAINT cliente_pkey PRIMARY KEY (id);


--
-- Name: item_peca item_peca_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_peca
    ADD CONSTRAINT item_peca_pkey PRIMARY KEY (id);


--
-- Name: item_servico item_servico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_servico
    ADD CONSTRAINT item_servico_pkey PRIMARY KEY (id);


--
-- Name: nota_fiscal nota_fiscal_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nota_fiscal
    ADD CONSTRAINT nota_fiscal_pkey PRIMARY KEY (id);


--
-- Name: ordem_servico ordem_servico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordem_servico
    ADD CONSTRAINT ordem_servico_pkey PRIMARY KEY (id);


--
-- Name: veiculo veiculo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.veiculo
    ADD CONSTRAINT veiculo_pkey PRIMARY KEY (id);


--
-- Name: veiculo veiculo_placa_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.veiculo
    ADD CONSTRAINT veiculo_placa_key UNIQUE (placa);


--
-- Name: idx_cliente_nome; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cliente_nome ON public.cliente USING gin (to_tsvector('portuguese'::regconfig, (nome)::text));


--
-- Name: idx_item_peca_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_item_peca_os ON public.item_peca USING btree (os_id);


--
-- Name: idx_item_servico_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_item_servico_os ON public.item_servico USING btree (os_id);


--
-- Name: idx_nf_ocr_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nf_ocr_status ON public.nota_fiscal USING btree (ocr_status);


--
-- Name: idx_nf_os; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_nf_os ON public.nota_fiscal USING btree (os_id);


--
-- Name: idx_os_data_entrada; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_data_entrada ON public.ordem_servico USING btree (data_entrada);


--
-- Name: idx_os_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_status ON public.ordem_servico USING btree (status);


--
-- Name: idx_os_veiculo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_os_veiculo ON public.ordem_servico USING btree (veiculo_id);


--
-- Name: idx_veiculo_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_veiculo_cliente ON public.veiculo USING btree (cliente_id);


--
-- Name: idx_veiculo_placa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_veiculo_placa ON public.veiculo USING btree (placa);


--
-- Name: item_peca trg_totais_peca; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_totais_peca AFTER INSERT OR DELETE OR UPDATE ON public.item_peca FOR EACH ROW EXECUTE FUNCTION public.recalcular_totais_os();


--
-- Name: item_servico trg_totais_servico; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_totais_servico AFTER INSERT OR DELETE OR UPDATE ON public.item_servico FOR EACH ROW EXECUTE FUNCTION public.recalcular_totais_os();


--
-- Name: item_peca item_peca_nf_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_peca
    ADD CONSTRAINT item_peca_nf_id_fkey FOREIGN KEY (nf_id) REFERENCES public.nota_fiscal(id);


--
-- Name: item_peca item_peca_os_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_peca
    ADD CONSTRAINT item_peca_os_id_fkey FOREIGN KEY (os_id) REFERENCES public.ordem_servico(id);


--
-- Name: item_servico item_servico_os_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.item_servico
    ADD CONSTRAINT item_servico_os_id_fkey FOREIGN KEY (os_id) REFERENCES public.ordem_servico(id);


--
-- Name: nota_fiscal nota_fiscal_os_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.nota_fiscal
    ADD CONSTRAINT nota_fiscal_os_id_fkey FOREIGN KEY (os_id) REFERENCES public.ordem_servico(id);


--
-- Name: ordem_servico ordem_servico_veiculo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordem_servico
    ADD CONSTRAINT ordem_servico_veiculo_id_fkey FOREIGN KEY (veiculo_id) REFERENCES public.veiculo(id);


--
-- Name: veiculo veiculo_cliente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.veiculo
    ADD CONSTRAINT veiculo_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES public.cliente(id);


--
-- PostgreSQL database dump complete
--



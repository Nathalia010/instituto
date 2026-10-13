-- Ejecutado una sola vez por Flyway. Los códigos de importación son estables.
CREATE TABLE usuarios (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    correo varchar(254) NOT NULL,
    password_hash text NOT NULL,
    rol varchar(20) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'PROFESOR', 'ESTUDIANTE')),
    activo boolean NOT NULL DEFAULT true,
    creado_en timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX usuarios_correo_unico ON usuarios (lower(correo));

CREATE TABLE estudiantes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id uuid UNIQUE REFERENCES usuarios(id),
    tipo_documento varchar(10) NOT NULL CHECK (tipo_documento IN ('CC','TI','CE','PA','PPT','RC')),
    documento varchar(40) NOT NULL CHECK (documento ~ '^[A-Z0-9]+$'),
    nombres varchar(120) NOT NULL CHECK (length(trim(nombres)) > 0),
    apellidos varchar(120) NOT NULL CHECK (length(trim(apellidos)) > 0),
    correo varchar(254),
    telefono varchar(30),
    activo boolean NOT NULL DEFAULT true,
    creado_en timestamptz NOT NULL DEFAULT now(),
    UNIQUE (tipo_documento, documento)
);

CREATE TABLE profesores (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id uuid UNIQUE REFERENCES usuarios(id),
    tipo_documento varchar(10) NOT NULL CHECK (tipo_documento IN ('CC','TI','CE','PA','PPT','RC')),
    documento varchar(40) NOT NULL CHECK (documento ~ '^[A-Z0-9]+$'),
    nombre_completo varchar(240) NOT NULL CHECK (length(trim(nombre_completo)) > 0),
    correo varchar(254) NOT NULL,
    telefono varchar(30),
    activo boolean NOT NULL DEFAULT true,
    UNIQUE (tipo_documento, documento)
);

CREATE TABLE cursos (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(180) NOT NULL CHECK (length(trim(nombre)) > 0),
    tipo_programa varchar(30) NOT NULL CHECK (tipo_programa IN ('VALIDACION_BACHILLERATO','CURSO','TECNICO')),
    duracion_horas integer NOT NULL CHECK (duracion_horas > 0),
    activo boolean NOT NULL DEFAULT true
);

CREATE TABLE periodos_academicos (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo varchar(40) NOT NULL UNIQUE,
    anio smallint NOT NULL CHECK (anio BETWEEN 2000 AND 2200),
    nombre varchar(100) NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    cerrado boolean NOT NULL DEFAULT false,
    CHECK (fecha_fin >= fecha_inicio),
    CHECK (extract(year FROM fecha_inicio) = anio)
);

-- Una oferta representa un grupo concreto de un curso en un periodo.
CREATE TABLE ofertas_curso (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    curso_id uuid NOT NULL REFERENCES cursos(id),
    periodo_id uuid NOT NULL REFERENCES periodos_academicos(id),
    grupo varchar(40) NOT NULL DEFAULT 'A',
    UNIQUE (curso_id, periodo_id, grupo)
);

CREATE TABLE matriculas (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo varchar(40) NOT NULL UNIQUE,
    estudiante_id uuid NOT NULL REFERENCES estudiantes(id),
    oferta_id uuid NOT NULL REFERENCES ofertas_curso(id),
    fecha_matricula date NOT NULL,
    estado_academico varchar(30) NOT NULL DEFAULT 'EN_CURSO' CHECK (estado_academico IN ('EN_CURSO','FINALIZADO','NO_FINALIZADO','RETIRADO','PENDIENTE_EVALUACION')),
    creado_en timestamptz NOT NULL DEFAULT now(),
    UNIQUE (estudiante_id, oferta_id)
);
CREATE INDEX matriculas_oferta_idx ON matriculas(oferta_id);

CREATE TABLE asignaciones_docentes (
    profesor_id uuid NOT NULL REFERENCES profesores(id),
    oferta_id uuid NOT NULL REFERENCES ofertas_curso(id),
    PRIMARY KEY (profesor_id, oferta_id)
);

-- La escala se define por evaluación; no se asume que todos califican de 0 a 5.
CREATE TABLE evaluaciones (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    oferta_id uuid NOT NULL REFERENCES ofertas_curso(id),
    codigo varchar(40) NOT NULL,
    nombre varchar(120) NOT NULL,
    nota_minima numeric(7,2) NOT NULL,
    nota_maxima numeric(7,2) NOT NULL,
    nota_aprobacion numeric(7,2) NOT NULL,
    UNIQUE (oferta_id, codigo),
    UNIQUE (id, oferta_id),
    CHECK (nota_maxima > nota_minima),
    CHECK (nota_aprobacion BETWEEN nota_minima AND nota_maxima)
);
ALTER TABLE matriculas ADD CONSTRAINT matriculas_id_oferta_unica UNIQUE (id, oferta_id);

CREATE TABLE notas (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    matricula_id uuid NOT NULL,
    evaluacion_id uuid NOT NULL,
    oferta_id uuid NOT NULL,
    profesor_id uuid NOT NULL,
    valor numeric(7,2) NOT NULL,
    registrado_por uuid NOT NULL REFERENCES usuarios(id),
    actualizado_en timestamptz NOT NULL DEFAULT now(),
    version integer NOT NULL DEFAULT 0,
    UNIQUE (matricula_id, evaluacion_id),
    FOREIGN KEY (matricula_id, oferta_id) REFERENCES matriculas(id, oferta_id),
    FOREIGN KEY (evaluacion_id, oferta_id) REFERENCES evaluaciones(id, oferta_id),
    FOREIGN KEY (profesor_id, oferta_id) REFERENCES asignaciones_docentes(profesor_id, oferta_id)
);

CREATE FUNCTION validar_nota() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE minimo numeric; maximo numeric;
BEGIN
    SELECT nota_minima, nota_maxima INTO minimo, maximo FROM evaluaciones WHERE id = NEW.evaluacion_id;
    IF NEW.valor < minimo OR NEW.valor > maximo THEN
        RAISE EXCEPTION 'Nota fuera de la escala de la evaluación';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER validar_nota_trigger BEFORE INSERT OR UPDATE ON notas FOR EACH ROW EXECUTE FUNCTION validar_nota();

CREATE TABLE conceptos_pago (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo varchar(40) NOT NULL UNIQUE,
    nombre varchar(120) NOT NULL,
    activo boolean NOT NULL DEFAULT true
);
CREATE TABLE cobros (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo varchar(40) NOT NULL UNIQUE,
    matricula_id uuid NOT NULL REFERENCES matriculas(id),
    concepto_id uuid NOT NULL REFERENCES conceptos_pago(id),
    valor numeric(14,2) NOT NULL CHECK (valor > 0),
    fecha_vencimiento date NOT NULL,
    registrado_por uuid NOT NULL REFERENCES usuarios(id),
    creado_en timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX cobros_matricula_idx ON cobros(matricula_id);
CREATE TABLE pagos (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    referencia varchar(100) NOT NULL UNIQUE,
    cobro_id uuid NOT NULL REFERENCES cobros(id),
    fecha date NOT NULL,
    valor numeric(14,2) NOT NULL CHECK (valor > 0),
    metodo varchar(20) NOT NULL CHECK (metodo IN ('EFECTIVO','TRANSFERENCIA','TARJETA','OTRO')),
    estado varchar(20) NOT NULL DEFAULT 'EN_REVISION' CHECK (estado IN ('CONFIRMADO','EN_REVISION')),
    registrado_por uuid NOT NULL REFERENCES usuarios(id),
    creado_en timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX pagos_cobro_idx ON pagos(cobro_id);

-- Se conservan los movimientos originales. Un cobro/pago incorrecto se revierte una vez.
CREATE TABLE reversiones_cobro (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cobro_id uuid NOT NULL UNIQUE REFERENCES cobros(id),
    motivo text NOT NULL CHECK (length(trim(motivo)) > 0),
    registrado_por uuid NOT NULL REFERENCES usuarios(id),
    creado_en timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE reversiones_pago (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    pago_id uuid NOT NULL UNIQUE REFERENCES pagos(id),
    motivo text NOT NULL CHECK (length(trim(motivo)) > 0),
    registrado_por uuid NOT NULL REFERENCES usuarios(id),
    creado_en timestamptz NOT NULL DEFAULT now()
);

CREATE VIEW saldos_matricula AS
WITH cobros_vigentes AS (
    SELECT c.* FROM cobros c WHERE NOT EXISTS (SELECT 1 FROM reversiones_cobro r WHERE r.cobro_id = c.id)
), pagos_validos AS (
    SELECT p.* FROM pagos p WHERE p.estado = 'CONFIRMADO'
    AND NOT EXISTS (SELECT 1 FROM reversiones_pago r WHERE r.pago_id = p.id)
), totales_cobros AS (
    SELECT matricula_id, sum(valor) AS total FROM cobros_vigentes GROUP BY matricula_id
), totales_pagos AS (
    SELECT c.matricula_id, sum(p.valor) AS total FROM pagos_validos p
    JOIN cobros_vigentes c ON c.id = p.cobro_id GROUP BY c.matricula_id
)
SELECT m.id AS matricula_id, coalesce(c.total,0) AS total_cobrado,
       coalesce(p.total,0) AS total_pagado,
       coalesce(c.total,0) - coalesce(p.total,0) AS saldo_pendiente
FROM matriculas m LEFT JOIN totales_cobros c ON c.matricula_id=m.id
LEFT JOIN totales_pagos p ON p.matricula_id=m.id;

CREATE TABLE importaciones (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id uuid NOT NULL REFERENCES usuarios(id),
    nombre_archivo varchar(255) NOT NULL,
    sha256 char(64) NOT NULL,
    modulo varchar(40) NOT NULL,
    mapeo_columnas jsonb NOT NULL DEFAULT '{}',
    estado varchar(20) NOT NULL CHECK (estado IN ('PREVISUALIZADA','CONFIRMADA','RECHAZADA','EXPIRADA')),
    filas_validas integer NOT NULL DEFAULT 0 CHECK (filas_validas >= 0),
    filas_error integer NOT NULL DEFAULT 0 CHECK (filas_error >= 0),
    creado_en timestamptz NOT NULL DEFAULT now(),
    confirmado_en timestamptz,
    CHECK ((estado = 'CONFIRMADA') = (confirmado_en IS NOT NULL))
);
CREATE TABLE errores_importacion (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    importacion_id uuid NOT NULL REFERENCES importaciones(id),
    fila integer NOT NULL CHECK (fila > 0),
    campo varchar(100),
    codigo varchar(40) NOT NULL,
    mensaje text NOT NULL
);
CREATE TABLE solicitudes_informacion (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    estudiante_id uuid NOT NULL REFERENCES estudiantes(id),
    curso_id uuid REFERENCES cursos(id),
    asunto varchar(200) NOT NULL,
    mensaje text NOT NULL,
    estado varchar(20) NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA','RESPONDIDA','CERRADA')),
    creado_en timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE historial_auditoria (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id uuid REFERENCES usuarios(id),
    accion varchar(60) NOT NULL,
    entidad varchar(60) NOT NULL,
    entidad_id uuid,
    motivo text,
    cambios jsonb NOT NULL DEFAULT '{}',
    creado_en timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX auditoria_fecha_idx ON historial_auditoria(creado_en);

CREATE FUNCTION impedir_modificacion_historica() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Movimiento histórico inmutable: registre una reversión';
END;
$$;
CREATE TRIGGER auditoria_inmutable BEFORE UPDATE OR DELETE ON historial_auditoria FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_historica();
CREATE TRIGGER cobros_inmutables BEFORE UPDATE OR DELETE ON cobros FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_historica();
CREATE TRIGGER pagos_inmutables BEFORE UPDATE OR DELETE ON pagos FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_historica();
CREATE TRIGGER reversiones_cobro_inmutables BEFORE UPDATE OR DELETE ON reversiones_cobro FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_historica();
CREATE TRIGGER reversiones_pago_inmutables BEFORE UPDATE OR DELETE ON reversiones_pago FOR EACH ROW EXECUTE FUNCTION impedir_modificacion_historica();

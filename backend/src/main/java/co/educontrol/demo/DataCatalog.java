package co.educontrol.demo;

import java.util.*;

final class DataCatalog {
 record Definition(String id,String label,boolean annual,List<String> fields,List<String> required) {}
 static Definition def(String id,String label,boolean annual,String fields,String required) {
  return new Definition(id,label,annual,List.of(fields.split(",")),List.of(required.split(",")));
 }
 static final List<Definition> ALL=List.of(
  def("estudiantes","Estudiantes",true,"tipo_documento,documento,nombres,apellidos,nombre_completo,correo,telefono,fecha_expedicion,programa,anio,estado_academico,notas_administrativas","tipo_documento,documento"),
  def("docentes","Docentes",false,"tipo_documento,documento,nombre_completo,correo,telefono","tipo_documento,documento,nombre_completo,correo"),
  def("cursos","Cursos",false,"codigo_curso,nombre,tipo_programa,duracion_horas,descripcion,publicado","codigo_curso,nombre,tipo_programa"),
  def("periodos","Periodos",true,"codigo_periodo,anio,nombre,fecha_inicio,fecha_fin","codigo_periodo,anio,nombre,fecha_inicio,fecha_fin"),
  def("matriculas","Matrículas",true,"codigo_matricula,tipo_documento,documento,codigo_curso,codigo_periodo,grupo,fecha_matricula,estado_academico","codigo_matricula,tipo_documento,documento,codigo_curso,codigo_periodo,grupo,fecha_matricula,estado_academico"),
  def("notas","Notas",true,"codigo_matricula,codigo_evaluacion,nota,tipo_documento_docente,documento_docente","codigo_matricula,codigo_evaluacion,nota,tipo_documento_docente,documento_docente"),
  def("cobros","Cobros",true,"codigo_cobro,codigo_matricula,codigo_concepto,valor_cop,fecha_vencimiento","codigo_cobro,codigo_matricula,codigo_concepto,valor_cop,fecha_vencimiento"),
  def("pagos","Pagos",true,"referencia,codigo_cobro,fecha_pago,valor_cop,metodo,estado_pago","referencia,codigo_cobro,fecha_pago,valor_cop,metodo"),
  def("agenda","Fechas y eventos",true,"codigo_evento,anio,tipo_evento,titulo,programa,calendario,etapa,fecha_inicio,fecha_fin,hora,lugar,enlace,detalle,estado","codigo_evento,anio,tipo_evento,titulo,fecha_inicio,estado")
 );
 static Definition get(String id) {return ALL.stream().filter(d->d.id().equals(id)).findFirst().orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Módulo inválido."));}
 static String key(String module,Map<String,String> r) {
  return switch(module) {
   case "estudiantes","docentes"->r.get("tipo_documento")+":"+r.get("documento");
   case "cursos"->r.get("codigo_curso"); case "periodos"->r.get("codigo_periodo");
   case "matriculas"->r.get("codigo_matricula"); case "notas"->r.get("codigo_matricula")+":"+r.get("codigo_evaluacion");
   case "cobros"->r.get("codigo_cobro"); case "pagos"->r.get("referencia"); case "agenda"->r.get("codigo_evento"); default->throw new IllegalArgumentException();
  };
 }
}

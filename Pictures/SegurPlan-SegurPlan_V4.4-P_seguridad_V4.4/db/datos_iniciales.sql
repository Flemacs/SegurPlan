-- =====================================================================
-- SEGURPLAN v5 - DATOS INICIALES (idempotente: se puede ejecutar varias veces)
--   psql -U postgres -d SegurPlan -f db/datos_iniciales.sql
--
-- USUARIOS DE PRUEBA (cámbielos antes de un uso real):
--   admin@segurplan.com    / Admin123!     (Administrador)
--   asesor@segurplan.com   / Asesor123!    (Asesor)
--   cliente@segurplan.com  / Cliente123!   (Cliente)
-- =====================================================================

-- Roles (los nombres deben coincidir con SecurityConfig)
INSERT INTO rol (nombre, descripcion) VALUES
  ('Cliente','Cliente / asegurado de la plataforma'),
  ('Asesor','Asesor comercial de SegurPlan'),
  ('Administrador','Administrador del sistema (incluye reportes y gerencia)')
ON CONFLICT (nombre) DO NOTHING;

-- Usuarios de prueba (contraseñas cifradas con BCrypt)
INSERT INTO usuario (nombres, apellidos, correo, telefono, password, estado, id_rol, area_especialidad)
SELECT v.nombres, v.apellidos, v.correo, v.telefono, v.pass, 'Activo', r.id_rol, v.area
FROM (VALUES
  ('Administrador','SegurPlan','admin@segurplan.com','999000001','$2a$10$jugvI4OYgB7zOfVeze48uePgar51XO/CJx5oAQp8ckEppdtUuDXPa','Administrador',NULL),
  ('Ana','Asesora Ventas','asesor@segurplan.com','999000002','$2a$10$NYf5VhYoS48AcOC39xVTKukgmAFgczbJ68G9dakOHYT/jWnXweZF2','Asesor','Siniestros'),
  ('Carlos','Cliente Prueba','cliente@segurplan.com','999000004','$2a$10$iDLC2pBWql.pKNhiKcvzUeJDw50g5a1TLKihzSYX.ZAwhUdY83Xfq','Cliente',NULL)
) AS v(nombres, apellidos, correo, telefono, pass, rol, area)
JOIN rol r ON r.nombre = v.rol
ON CONFLICT (correo) DO NOTHING;

-- Tipos de seguro
INSERT INTO tipo_seguro (nombre, descripcion) VALUES
  ('Seguro vehicular','Daños propios y a terceros para su vehículo'),
  ('SOAT','Seguro Obligatorio de Accidentes de Tránsito'),
  ('Seguro de vida','Protección económica para su familia'),
  ('Seguro de vida con ahorro','Cobertura de vida con componente de ahorro'),
  ('Seguro de salud','Atención médica y hospitalaria'),
  ('Seguro oncológico','Cobertura para diagnóstico y tratamiento oncológico'),
  ('Seguro de hogar','Protección de su vivienda y contenido'),
  ('Seguro de accidentes personales','Cobertura ante accidentes'),
  ('Seguro de viaje','Asistencia y cobertura en viajes'),
  ('Seguro de desgravamen','Cancela deudas ante fallecimiento o invalidez'),
  ('Seguro agrícola','Protección de cultivos'),
  ('Seguro estudiantil','Cobertura para estudiantes'),
  ('Seguro de sepelio','Gastos de sepelio'),
  ('Seguro de desempleo','Cobertura ante pérdida involuntaria de empleo')
ON CONFLICT (nombre) DO NOTHING;

-- Aseguradoras
INSERT INTO aseguradora (nombre, ruc, telefono, correo) VALUES
  ('Rímac Seguros','20100041953','012111000','contacto@rimac.example'),
  ('Pacífico Seguros','20100035392','012222000','contacto@pacifico.example'),
  ('La Positiva','20100210909','012333000','contacto@lapositiva.example'),
  ('Mapfre Perú','20202380621','012444000','contacto@mapfre.example'),
  ('Interseguro','20100053455','012555000','contacto@interseguro.example'),
  ('Seguros SURA','20517207331','012666000','contacto@sura.example'),
  ('BNP Paribas Cardif','20513328644','012777000','contacto@cardif.example')
ON CONFLICT (ruc) DO NOTHING;

-- Matriz de permisos por defecto (editable desde Administración > Roles y permisos)
INSERT INTO rol_permiso (id_rol, modulo, ver, crear, editar, eliminar)
SELECT r.id_rol, p.modulo, p.ver, p.crear, p.editar, p.eliminar
FROM (VALUES
  ('Cliente','Cotizaciones',true,true,false,false),
  ('Cliente','Pólizas',true,false,false,false),
  ('Cliente','AFP',true,true,false,false),
  ('Cliente','Siniestros',true,true,false,false),
  ('Cliente','Documentos',true,true,false,true),
  ('Asesor','Cotizaciones',true,false,true,false),
  ('Asesor','Pólizas',true,false,false,false),
  ('Asesor','AFP',true,false,true,false),
  ('Asesor','Siniestros',true,false,true,false),
  ('Asesor','Documentos',true,false,true,false),
  ('Asesor','Historial',true,false,false,false)
) AS p(rol, modulo, ver, crear, editar, eliminar)
JOIN rol r ON r.nombre = p.rol
ON CONFLICT (id_rol, modulo) DO NOTHING;

INSERT INTO rol_permiso (id_rol, modulo, ver, crear, editar, eliminar)
SELECT r.id_rol, m.modulo, true, true, true, true
FROM rol r
CROSS JOIN (VALUES ('Cotizaciones'),('Pólizas'),('AFP'),('Siniestros'),('Documentos'),
  ('Usuarios'),('Asesores'),('Roles'),('Reportes'),('Historial'),('Respaldo'),('Catálogos')) AS m(modulo)
WHERE r.nombre = 'Administrador'
ON CONFLICT (id_rol, modulo) DO NOTHING;

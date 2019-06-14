select a.i_attr_def as tbl 
from dmc_aspect_type_s a,
	dm_sysobject_s s 
where s.r_object_id = a.r_object_id and 
	s.object_name = 'si_aspect'

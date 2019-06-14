create view dbo.si_view
as
	select a.r_object_id, 
		a.si_value as si1, 
		b.r_object_id as similar_obj_id, 
		b.si_value as si2, 
		dbo.HamDist(a.si_value, b.si_value) as distance, 
		(64.0 - dbo.HamDist(a.si_value, b.si_value)) / 64 as similarity
from [dmi_table_name]_s as a 
	inner join [dmi_table_name]_s as b 
		on a.r_object_id <> b.r_object_id
where (a.si_value <> '') and (b.si_value <> '') and 
	((64.0 - dbo.HamDist(a.si_value, b.si_value)) / 64 >= 0.7)


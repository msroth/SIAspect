create function HamDist(@value1 char(64), @value2 char(64))
returns int 

as 
begin 
declare @distance int 
declare @i int 
declare @len int 

select @distance = 0, 
@i =1, 
@len = case when len(@value1) > len(@value2) 
then len(@value1) 
else len(@value2) end

if (@value1 is null) or (@value2 is null) 
return null 

while (@i <= @len) 
select @distance = @distance + 
case when substring(@value1,@i,1) != 
substring(@value2,@i,1)
then 1 
else 0 end, 
@i = @i +1 

return @distance 
end

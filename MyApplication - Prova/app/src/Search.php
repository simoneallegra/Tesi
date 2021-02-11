<?php

#password di 000webhost: 5N1mPpc!gFj#e3DNFl(Z

#password di 000webhost/gestore database: Ld%[[\gaj0H6?E9|


define('HOST','localhost');
define('USER','user_name');
define('PASS','password');
define('DB','provatesi');

$con = mysqli_connect(HOST, USER, PASS,DB);
$elem  = $_GET['elem'];
 
$sql = "select * from tabella1 where Title regexp '%$elem%'";
 
$res = mysqli_query($con,$sql);
 
$result = array();
 
while($row = mysqli_fetch_array($res)){
array_push($result,array('title'=>$row[2],
'date'=>$row[1],
'data'=>$row[3],
'id'=>$row[0]

));
}
 
echo json_encode(array("result"=>$result));
 
mysqli_close($con);
 
?>
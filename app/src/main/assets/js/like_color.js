function changeColor(e){
 e.style.color = "rgb(64, 128, 255)";
}

function populate() {
   for(var i=1;i<=30;i++) {
      var x=document.getElementById("#u_ps_0_0_a").innerHTML;
      var p="<style='color: ' role='button' value='"+i+"'onclick='changeColor(this)'/>";
      document.getElementById("#u_ps_0_0_a").innerHTML+=p;
   }
}

populate() ;
$(function(){
  window.setInterval(function(){
    $.get('/data', function(data){
      render(data);
    });
  }, AIR_ISU_REFRESH_INTERVAL);
});

function render(list) {
  $('#api-response-container .api-result').remove();
  var api_results = [];
  list.forEach(function(item){
    var element = $('#api-result-template-container .api-result').clone();
    switch(item.service) {
      case 'ken':
      case 'ken2': render_ken(element, item.data); break;
      default: console.log("unknown api response, service:" + item.service); console.log(item.data);
    }
    $('#api-response-container').append(element);
  });
}

function render_ken(element, data){
  $(element).find('h4').text("KEN: " + data.zipcode);
  $(element).find('p').text(data.addresses.join(", "));
}

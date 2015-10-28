$(function(){
  window.setInterval(function(){
    $.get('/data', function(data){ render(data); });
  }, AIR_ISU_REFRESH_INTERVAL);
  $.get('/data', function(data){ render(data); });
});

function render(list) {
  $('#api-response-container .api-result').remove();
  var api_results = [];
  list.forEach(function(item){
    var element = $('#api-result-template-container .api-result').clone();
    switch(item.service) {
      case 'ken':
      case 'ken2': render_ken(element, item.data); break;
      case 'surname':
      case 'givenname': render_name(element, item.data); break;
      case 'tenki': render_tenki(element, item.data); break;
      case 'perfectsec': render_perfectsec(element, item.data); break;
      case 'perfectsec_attacked': render_perfectsec_attacked(element, item.data); break;
      default: console.log("unknown api response, service:" + item.service); console.log(item.data);
    }
    $('#api-response-container').append(element);
  });
}

function render_tenki(element, data){
  $(element).find('h4').text("Updated: " + data.date);
  $(element).find('p').text(data.yoho);
}

function render_perfectsec(element, data){
  $(element).find('h4').text("Request: " + data.req);
  $(element).find('p').text("KEY: " + data.key + ", TOKEN: " + data.onetime_token);
}

function render_perfectsec_attacked(element, data){
  var updatedAt = new Date(data.updated_at * 1000);
  $(element).find('h4').text("updated: " + updatedAt.toISOString());
  $(element).find('p').text(data.key1 + ", " + data.key2 + ", " + data.key3);
}

function render_ken(element, data){
  $(element).find('h4').text("KEN: " + data.zipcode);
  $(element).find('p').text(data.addresses.join(", "));
}

function render_name(element, data){
  $(element).find('h4').text("NAME: " + data.query);
  var names = [];
  data.result.forEach(function(item){
    var yomi = item.yomi;
    var name = item.name;
    names.push(name + "（" + yomi + "）");
  });
  $(element).find('p').text(names.join(", "));
}


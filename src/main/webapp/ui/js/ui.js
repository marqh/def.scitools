// Set of initialization actions fun on page load to make UI features live

$(function() {

    // Query forms run a target query and load the resulting HTML into a data-result element
    var processQueryForms = function() {
        $(".query-form").each(function() {
            var target = $(this).attr('data-target');
            var queryField = $(this).attr('data-query');
            var resultField = $(this).attr('data-result');
            $(this).submit( function(){
                var url = target + $(queryField).val();
                $(resultField).load(url);
                return false;
            });
        });
    };

    processQueryForms();

    // Anything marked as popinfo will have a popover (data-trigger, data-placement, data-content)
    // enable popups
    $(".popinfo").popover();

    // General ajax forms which reload the page when actioned. Errors are displayed in elts of class "ajax-error".
    $(".ajax-form").ajaxForm({
      success:
          function(data, status, xhr){
            // $("#status-dialog").modal("hide");
            location.reload();
          },

      error:
        function(xhr, status, error){
           $(".ajax-error").html("<div class='alert'> <button type='button' class='close' data-dismiss='alert'>&times;</button>Action failed: " + error + " - " + xhr.responseText + "</div>");
        }
    });

    // Set up editable fields
    $.fn.editable.defaults.mode = 'inline';

    var editRemoveAction = function(e){
        var rowid = $(e.target).attr("data-target");
        $(rowid).remove();
    };

    // Machinery to run edit-form interactions
    var makeEditCell = function(name, value) {
        return '<td><a href="#" class="ui-editable" data-type="text" data-inputclass="input-large" data-name="' + name + '">' + value + '</a></td>';
    }
    var makeEditRow = function(id, prop, value) {
        var row =
            '<tr id="$id">' + makeEditCell("prop", prop) + makeEditCell(prop, value)
            + '<td><a class="edit-remove-row"><i data-target="#$id"  class="icon-minus-sign"></i></a>   \n'
            +     '<a class="edit-add-row"><i data-target="#$id"  class="icon-plus-sign"></i></a></td></tr>';
        row = row.replace(/\$id/g, id);
        return row;
    };

    var installEditRow = function(position, id, prop, value) {
        position.after( makeEditRow(id, prop, '""') );
        $("#" + id).each(function(){
            $(this).find(".ui-editable").editable();
            $(this).find(".edit-remove-row").click( editRemoveAction );
            $(this).find(".edit-add-row").click( editAddAction );
            $(this).find('.ui-editable[data-name="prop"]').on('save', function(){
                var that = this;
                setTimeout(function() {
                    $(that).closest('td').next().find('.ui-editable').editable('show');
                }, 200);
            });
        });
    };
    
    var idcount = 1;

    var editAddAction = function(e){
        var row = $($(e.target).attr("data-target"));
        var newid = row.attr("id") + idcount++;
        var prop = row.find("td:first").text();
        installEditRow(row, newid, prop, '""');
        return false;
    }
    
    var editAddNewAction = function(e) {
        var tableid = $(e.target).attr("data-target");
        var lastrow = $(tableid).find("tbody tr:last");
        var newid =  (tableid.replace(/^#/,'')) + "-newrow-"+ idcount++;
        installEditRow(lastrow, newid, '', '""');
        return false;
    }

    $(".ui-editable").editable();
    $(".edit-remove-row").click( editRemoveAction );
    $(".edit-add-row").click( editAddAction );
    $(".edit-add-newrow").click( editAddNewAction );

    var emitResource = function(val) {
        if (val.match(/https?:/)) {
            return "<" + val + ">";
        } else {
            return val;
        }
    };

    // Implement edit save-changes functionality
    $(".edit-table-save").click( function(){
        var id = $(this).attr("data-id");
        var table = $("#" + id + "-edit-table");
        var data = $("#" + id + "-edit-prefixes").text();
        var url = table.attr("data-target");
        data = data + "\n<" + table.attr("data-root") + ">\n";
        table.find("tbody tr").each(function(){
            var row = $(this).find("td").toArray();
            var prop = emitResource($(row[0]).text());
            var value = $(row[1]).text();
            data = data + "    " + prop + " " + value + " ;\n";
        });
        $.ajax({
            type: "PUT",
            url: url,
            data: data,
            contentType: "text/turtle",
            success: function(){
                $("#" + id + "-msg").html("Submitted successfully");
                location.reload();
            },
            error: function(xhr, status, error){
                $("#" + id + "-msg").html("Save failed: " + error + " - " + xhr.responseText);
                $('#" + id + "-msg-alert').removeClass('alert-success').addClass('alert-error').show();
            }
          });
    });
});